"use strict";

const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const {before, after, beforeEach} = require("node:test");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {ref, set, update, get, query, orderByChild, equalTo} = require("firebase/database");

let testEnv;
const ADMIN_EMAIL = "mdomor01815@gmail.com";

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-foodmobo",
    database: {
      rules: fs.readFileSync(path.join(__dirname, "../../firebase-database-rules.json"), "utf8"),
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearDatabase();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.database();
    await set(ref(db, "users/buyer-1"), profile("buyer-1", "buyer@example.com", "buyer", "active"));
    await set(ref(db, "users/vendor-1"), profile("vendor-1", "vendor@example.com", "vendor", "approved"));
    await set(ref(db, "users/vendor-2"), profile("vendor-2", "vendor2@example.com", "vendor", "pending"));
    await set(ref(db, "vendorApplications/vendor-2"),
        profile("vendor-2", "vendor2@example.com", "vendor", "pending"));
    await set(ref(db, "foods/food-1"), food("food-1", "vendor-1", 100));
    await set(ref(db, "rentalCarts/cart-1"), {
      id: "cart-1",
      name: "Starter Cart",
      location: "Dhaka",
      description: "Safe mobile cart",
      dailyRate: 500,
      available: true,
    });
  });
});

function profile(uid, email, role, status) {
  return {uid, name: uid, email, role, status, rating: 0, createdAt: 1};
}

function food(id, vendorId, price) {
  return {
    id,
    vendorId,
    vendorName: "Vendor One",
    name: "Rice",
    category: "Rice",
    description: "Fresh rice",
    price,
    rating: 0,
    available: true,
    createdAt: 1,
  };
}

function cartLine(price = 100) {
  return {
    foodId: "food-1",
    vendorId: "vendor-1",
    vendorName: "Vendor One",
    name: "Rice",
    unitPrice: price,
    quantity: 2,
  };
}

function order(status = "placed") {
  return {
    id: "order-1",
    buyerId: "buyer-1",
    vendorId: "vendor-1",
    buyerName: "buyer-1",
    address: "Dhaka delivery address",
    status,
    createdAt: 1000,
    updatedAt: 1000,
    items: {"food-1": cartLine()},
  };
}

function authDb(uid, email, verified = true) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: verified,
  }).database();
}

test("unauthenticated users cannot browse foods", async () => {
  await assertFails(get(ref(testEnv.unauthenticatedContext().database(), "foods")));
});

test("a user can create a constrained profile but cannot promote the role", async () => {
  const db = authDb("new-buyer", "new@example.com");
  await assertSucceeds(set(ref(db, "users/new-buyer"),
      profile("new-buyer", "new@example.com", "buyer", "active")));
  await assertSucceeds(update(ref(db, "users/new-buyer"), {name: "Updated Buyer"}));
  await assertFails(update(ref(db, "users/new-buyer"), {role: "admin"}));
  await assertFails(update(ref(db, "users/new-buyer"), {status: "approved"}));
});

test("only approved vendors or the verified admin can publish food", async () => {
  const approved = authDb("vendor-1", "vendor@example.com");
  await assertSucceeds(set(ref(approved, "foods/food-2"), {
    ...food("food-2", "vendor-1", 120),
    name: "Momo",
  }));

  const pending = authDb("vendor-2", "vendor2@example.com");
  await assertFails(set(ref(pending, "foods/food-3"), {
    ...food("food-3", "vendor-2", 90),
    name: "Soup",
  }));

  const admin = authDb("admin-1", ADMIN_EMAIL);
  await assertSucceeds(set(ref(admin, "foods/admin-food"), {
    ...food("admin-food", "admin-1", 150),
    name: "Admin Meal",
  }));
});

test("cart and order prices must match the official menu", async () => {
  const db = authDb("buyer-1", "buyer@example.com");
  await assertSucceeds(set(ref(db, "carts/buyer-1/food-1"), cartLine()));
  await assertFails(set(ref(db, "carts/buyer-1/food-1"), cartLine(1)));

  await assertSucceeds(set(ref(db, "orders/order-1"), order()));
  await assertFails(set(ref(db, "orders/order-bad"), {
    ...order(),
    id: "order-bad",
    items: {"food-1": cartLine(1)},
  }));
});

test("buyers and vendors can query only their canonical orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), "orders/order-1"), order());
  });
  const buyer = authDb("buyer-1", "buyer@example.com");
  await assertSucceeds(get(query(ref(buyer, "orders"), orderByChild("buyerId"), equalTo("buyer-1"))));
  await assertFails(get(ref(buyer, "orders")));

  const vendor = authDb("vendor-1", "vendor@example.com");
  await assertSucceeds(get(query(ref(vendor, "orders"), orderByChild("vendorId"), equalTo("vendor-1"))));
});

test("only the vendor can advance the order through the allowed sequence", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), "orders/order-1"), order());
  });
  const buyer = authDb("buyer-1", "buyer@example.com");
  await assertFails(update(ref(buyer, "orders/order-1"), {status: "accepted", updatedAt: 2000}));

  const vendor = authDb("vendor-1", "vendor@example.com");
  await assertSucceeds(update(ref(vendor, "orders/order-1"), {status: "accepted", updatedAt: 2000}));
  await assertFails(update(ref(vendor, "orders/order-1"), {status: "delivered", updatedAt: 3000}));
});

test("a delivered order allows one deterministic review from each participant", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), "orders/order-1"), order("delivered"));
  });
  const buyer = authDb("buyer-1", "buyer@example.com");
  const review = {
    id: "order-1_buyer-1",
    orderId: "order-1",
    authorId: "buyer-1",
    targetUserId: "vendor-1",
    stars: 5,
    comment: "Excellent service",
    createdAt: 2000,
  };
  await assertSucceeds(set(ref(buyer, "reviews/order-1_buyer-1"), review));
  await assertFails(set(ref(buyer, "reviews/order-1_buyer-1"), {...review, stars: 1}));
  await assertFails(set(ref(buyer, "reviews/wrong-id"), review));
});

test("daily rental reservations prevent overlapping bookings", async () => {
  const buyer = authDb("buyer-1", "buyer@example.com");
  const booking = {
    id: "booking-1",
    cartId: "cart-1",
    cartName: "Starter Cart",
    userId: "buyer-1",
    requestedLocation: "Dhanmondi",
    status: "holding",
    startAt: Date.now() + 86400000,
    endAt: Date.now() + 3 * 86400000,
    createdAt: 1000,
    updatedAt: 1000,
    days: 2,
    delivery: false,
    dailyRate: 500,
    total: 1000,
  };
  booking.endAt = booking.startAt + booking.days * 86400000;
  await assertSucceeds(set(ref(buyer, "rentalBookings/booking-1"), booking));
  const reservation = {bookingId: "booking-1", userId: "buyer-1", createdAt: 1000};
  await assertSucceeds(set(ref(buyer, "rentalReservations/cart-1/20260725"), reservation));

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), "rentalBookings/booking-2"), {
      ...booking,
      id: "booking-2",
    });
  });
  const secondBuyer = authDb("buyer-2", "buyer2@example.com");
  await assertFails(set(ref(secondBuyer, "rentalReservations/cart-1/20260725"), {
    bookingId: "booking-2",
    userId: "buyer-2",
    createdAt: 1000,
  }));
});

test("the verified administrator can approve a pending vendor", async () => {
  const admin = authDb("admin-1", ADMIN_EMAIL);
  await assertSucceeds(update(ref(admin), {
    "users/vendor-2/status": "approved",
    "vendorApplications/vendor-2/status": "approved",
  }));
});
