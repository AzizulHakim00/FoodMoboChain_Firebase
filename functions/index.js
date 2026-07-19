"use strict";

const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onValueCreated} = require("firebase-functions/v2/database");
const {defineString} = require("firebase-functions/params");
const admin = require("firebase-admin");
const {
  nextOrderStatus,
  normalizeAddress,
  validateStars,
  buildVendorOrders,
  rangesOverlap,
} = require("./lib/domain");

admin.initializeApp();
const db = admin.database();
const ADMIN_EMAIL = defineString("ADMIN_EMAIL");

function requireAuth(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  return request.auth;
}

function requireVerifiedEmail(request) {
  const auth = requireAuth(request);
  if (auth.token.email_verified !== true) {
    throw new HttpsError("failed-precondition", "Verify your email first.");
  }
  return auth;
}

function requireAdmin(request) {
  const auth = requireVerifiedEmail(request);
  if (auth.token.admin !== true) {
    throw new HttpsError("permission-denied", "Administrator access is required.");
  }
  return auth;
}

exports.bootstrapAdmin = onCall(async (request) => {
  const auth = requireVerifiedEmail(request);
  const configuredEmail = ADMIN_EMAIL.value().trim().toLowerCase();
  const signedInEmail = String(auth.token.email || "").trim().toLowerCase();
  if (!configuredEmail || signedInEmail !== configuredEmail) {
    return {admin: false};
  }

  const [userRecord, profileSnapshot] = await Promise.all([
    admin.auth().getUser(auth.uid),
    db.ref(`users/${auth.uid}`).get(),
  ]);
  const claims = {...(userRecord.customClaims || {}), admin: true, role: "admin"};
  await admin.auth().setCustomUserClaims(auth.uid, claims);
  const profileUpdate = {
    uid: auth.uid,
    email: signedInEmail,
    role: "admin",
    status: "active",
    updatedAt: Date.now(),
  };
  if (!profileSnapshot.child("name").exists()) {
    profileUpdate.name = userRecord.displayName || "FoodMoboChain Admin";
  }
  if (!profileSnapshot.child("createdAt").exists()) {
    profileUpdate.createdAt = Date.now();
  }
  await db.ref(`users/${auth.uid}`).update(profileUpdate);
  return {admin: true};
});

exports.approveVendor = onCall(async (request) => {
  requireAdmin(request);
  const uid = String(request.data && request.data.uid || "").trim();
  const status = String(request.data && request.data.status || "").trim();
  if (!uid || !["approved", "rejected"].includes(status)) {
    throw new HttpsError("invalid-argument", "A vendor UID and valid status are required.");
  }

  const userRecord = await admin.auth().getUser(uid);
  const claims = {...(userRecord.customClaims || {})};
  if (status === "approved") {
    claims.vendor = true;
    claims.role = "vendor";
  } else {
    delete claims.vendor;
    if (claims.role === "vendor") delete claims.role;
  }
  await admin.auth().setCustomUserClaims(uid, claims);
  await db.ref().update({
    [`users/${uid}/status`]: status,
    [`vendorApplications/${uid}/status`]: status,
    [`vendorApplications/${uid}/reviewedAt`]: Date.now(),
  });
  return {uid, status};
});

exports.placeOrder = onCall(async (request) => {
  const auth = requireVerifiedEmail(request);
  const address = normalizeAddress(request.data && request.data.address);
  if (address.length < 5 || address.length > 300) {
    throw new HttpsError("invalid-argument", "Enter a valid delivery address.");
  }

  const [cartSnapshot, buyerSnapshot] = await Promise.all([
    db.ref(`carts/${auth.uid}`).get(),
    db.ref(`users/${auth.uid}`).get(),
  ]);
  const cart = cartSnapshot.val();
  if (!cart) throw new HttpsError("failed-precondition", "Your bag is empty.");

  const foodIds = Object.entries(cart).map(([key, line]) => line && line.foodId || key);
  const foodSnapshots = await Promise.all(foodIds.map((foodId) => db.ref(`foods/${foodId}`).get()));
  const foods = {};
  foodSnapshots.forEach((snapshot, index) => {
    if (snapshot.exists()) foods[foodIds[index]] = snapshot.val();
  });

  let orders;
  try {
    orders = buildVendorOrders({
      cart,
      foods,
      buyerId: auth.uid,
      buyerName: buyerSnapshot.child("name").val() || "Buyer",
      address,
      now: Date.now(),
      idFactory: () => db.ref("orders").push().key,
    });
  } catch (error) {
    throw new HttpsError("failed-precondition", error.message);
  }

  const vendorSnapshots = await Promise.all(orders.map((order) =>
    db.ref(`users/${order.vendorId}`).get()));
  vendorSnapshots.forEach((snapshot, index) => {
    const status = snapshot.child("status").val();
    const role = snapshot.child("role").val();
    if (!(status === "approved" || (status === "active" && role === "admin"))) {
      throw new HttpsError("failed-precondition",
          `Vendor for order ${orders[index].id} is not approved.`);
    }
  });

  const updates = {};
  let grandTotal = 0;
  for (const order of orders) {
    grandTotal += order.total;
    updates[`orders/${order.id}`] = order;
    updates[`userOrders/${auth.uid}/${order.id}`] = order;
    updates[`vendorOrders/${order.vendorId}/${order.id}`] = order;
  }
  updates[`carts/${auth.uid}`] = null;
  await db.ref().update(updates);
  return {orderIds: orders.map((order) => order.id), orderCount: orders.length, total: grandTotal};
});

exports.bookRental = onCall(async (request) => {
  const auth = requireVerifiedEmail(request);
  const cartId = String(request.data && request.data.cartId || "").trim();
  const requestedLocation = String(request.data && request.data.location || "").trim();
  const startAt = Number(request.data && request.data.startAt);
  const days = Number(request.data && request.data.days);
  const delivery = request.data && request.data.delivery === true;
  if (!cartId || requestedLocation.length < 2 || requestedLocation.length > 200) {
    throw new HttpsError("invalid-argument", "Cart and delivery location are required.");
  }
  if (!Number.isInteger(days) || days < 1 || days > 90 || !Number.isFinite(startAt)) {
    throw new HttpsError("invalid-argument", "Use a valid start date and a 1–90 day duration.");
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (startAt < today.getTime() - 86_400_000) {
    throw new HttpsError("failed-precondition", "Rental start date cannot be in the past.");
  }
  const endAt = startAt + days * 86_400_000;
  const cartSnapshot = await db.ref(`rentalCarts/${cartId}`).get();
  if (!cartSnapshot.exists() || cartSnapshot.child("available").val() !== true) {
    throw new HttpsError("failed-precondition", "This cart is unavailable.");
  }
  const cart = cartSnapshot.val();
  const bookingId = db.ref("rentalBookings").push().key;
  if (!bookingId) throw new HttpsError("internal", "Could not create a booking ID.");

  const reservationRef = db.ref(`rentalReservations/${cartId}`);
  const reservationResult = await reservationRef.transaction((current) => {
    const reservations = current || {};
    for (const reservation of Object.values(reservations)) {
      if (reservation && rangesOverlap(startAt, endAt,
          Number(reservation.startAt), Number(reservation.endAt))) {
        return;
      }
    }
    reservations[bookingId] = {bookingId, userId: auth.uid, startAt, endAt};
    return reservations;
  }, undefined, false);
  if (!reservationResult.committed) {
    throw new HttpsError("already-exists", "That cart is already reserved for the selected dates.");
  }

  const booking = {
    id: bookingId,
    cartId,
    cartName: cart.name || "Food cart",
    userId: auth.uid,
    requestedLocation,
    status: "requested",
    startAt,
    endAt,
    days,
    delivery,
    total: Number(cart.dailyRate || 0) * days + (delivery ? 300 : 0),
    createdAt: Date.now(),
  };
  try {
    await db.ref().update({
      [`rentalBookings/${bookingId}`]: booking,
      [`userRentalBookings/${auth.uid}/${bookingId}`]: booking,
    });
  } catch (error) {
    await reservationRef.child(bookingId).remove();
    throw error;
  }
  return {bookingId, total: booking.total};
});

exports.advanceOrderStatus = onCall(async (request) => {
  const auth = requireVerifiedEmail(request);
  const orderId = String(request.data && request.data.orderId || "").trim();
  if (!orderId) throw new HttpsError("invalid-argument", "Order ID is required.");

  const snapshot = await db.ref(`orders/${orderId}`).get();
  if (!snapshot.exists()) throw new HttpsError("not-found", "Order not found.");
  const order = snapshot.val();
  const isAdmin = auth.token.admin === true;
  if (!isAdmin && order.vendorId !== auth.uid) {
    throw new HttpsError("permission-denied", "Only this order's vendor can update it.");
  }
  const next = nextOrderStatus(order.status);
  if (!next) throw new HttpsError("failed-precondition", "This order cannot advance further.");

  await db.ref().update({
    [`orders/${orderId}/status`]: next,
    [`orders/${orderId}/updatedAt`]: Date.now(),
    [`userOrders/${order.buyerId}/${orderId}/status`]: next,
    [`userOrders/${order.buyerId}/${orderId}/updatedAt`]: Date.now(),
    [`vendorOrders/${order.vendorId}/${orderId}/status`]: next,
    [`vendorOrders/${order.vendorId}/${orderId}/updatedAt`]: Date.now(),
  });
  return {orderId, status: next};
});

exports.submitReview = onCall(async (request) => {
  const auth = requireVerifiedEmail(request);
  const orderId = String(request.data && request.data.orderId || "").trim();
  const comment = String(request.data && request.data.comment || "").trim();
  let stars;
  try {
    stars = validateStars(request.data && request.data.stars);
  } catch (error) {
    throw new HttpsError("invalid-argument", error.message);
  }
  if (!orderId || comment.length < 3 || comment.length > 500) {
    throw new HttpsError("invalid-argument", "Order ID and a 3–500 character comment are required.");
  }

  const orderSnapshot = await db.ref(`orders/${orderId}`).get();
  if (!orderSnapshot.exists()) throw new HttpsError("not-found", "Order not found.");
  const order = orderSnapshot.val();
  if (order.status !== "delivered") {
    throw new HttpsError("failed-precondition", "Reviews are available after delivery.");
  }

  let targetUserId;
  if (auth.uid === order.buyerId) targetUserId = order.vendorId;
  else if (auth.uid === order.vendorId) targetUserId = order.buyerId;
  else throw new HttpsError("permission-denied", "You are not part of this order.");
  if (!targetUserId || targetUserId === auth.uid) {
    throw new HttpsError("failed-precondition", "A valid review target is required.");
  }

  const reviewId = `${orderId}_${auth.uid}`;
  const reviewRef = db.ref(`reviews/${reviewId}`);
  const transaction = await reviewRef.transaction((current) => {
    if (current) return;
    return {
      id: reviewId,
      orderId,
      authorId: auth.uid,
      targetUserId,
      stars,
      comment,
      createdAt: Date.now(),
    };
  }, undefined, false);
  if (!transaction.committed) {
    throw new HttpsError("already-exists", "You already reviewed this order.");
  }
  return {reviewId, targetUserId};
});

exports.aggregateReviewRating = onValueCreated("reviews/{reviewId}", async (event) => {
  const review = event.data.val();
  if (!review || !review.targetUserId) return;
  const reviewsSnapshot = await db.ref("reviews")
      .orderByChild("targetUserId")
      .equalTo(review.targetUserId)
      .get();
  let sum = 0;
  let count = 0;
  reviewsSnapshot.forEach((child) => {
    const value = child.val();
    const stars = Number(value && value.stars);
    if (Number.isInteger(stars) && stars >= 1 && stars <= 5) {
      sum += stars;
      count += 1;
    }
  });
  const rating = count > 0 ? sum / count : 0;
  await db.ref().update({
    [`ratingStats/${review.targetUserId}`]: {sum, count},
    [`users/${review.targetUserId}/rating`]: rating,
  });
});
