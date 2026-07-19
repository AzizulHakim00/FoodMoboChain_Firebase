"use strict";

const ORDER_TRANSITIONS = Object.freeze({
  placed: "accepted",
  accepted: "preparing",
  preparing: "out_for_delivery",
  out_for_delivery: "delivered",
});

function nextOrderStatus(currentStatus) {
  return ORDER_TRANSITIONS[currentStatus] || null;
}

function normalizeAddress(value) {
  if (typeof value !== "string") return "";
  return value.trim().replace(/\s+/g, " ");
}

function validateStars(value) {
  const stars = Number(value);
  if (!Number.isInteger(stars) || stars < 1 || stars > 5) {
    throw new Error("Rating must be an integer between 1 and 5.");
  }
  return stars;
}

function buildVendorOrders({cart, foods, buyerId, buyerName, address, now, idFactory}) {
  if (!cart || typeof cart !== "object" || Object.keys(cart).length === 0) {
    throw new Error("The cart is empty.");
  }
  const normalizedAddress = normalizeAddress(address);
  if (normalizedAddress.length < 5 || normalizedAddress.length > 300) {
    throw new Error("A valid delivery address is required.");
  }

  const groups = new Map();
  for (const [cartKey, rawLine] of Object.entries(cart)) {
    const foodId = rawLine && (rawLine.foodId || cartKey);
    const quantity = Number(rawLine && rawLine.quantity);
    const food = foods[foodId];
    if (!food || food.available !== true) {
      throw new Error(`Food item ${foodId} is unavailable.`);
    }
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 20) {
      throw new Error(`Invalid quantity for ${foodId}.`);
    }
    if (!food.vendorId) {
      throw new Error(`Food item ${foodId} has no vendor.`);
    }

    const vendorId = String(food.vendorId);
    if (!groups.has(vendorId)) groups.set(vendorId, []);
    groups.get(vendorId).push({
      foodId,
      vendorId,
      vendorName: food.vendorName || "Vendor",
      name: food.name || "Food item",
      unitPrice: Number(food.price),
      quantity,
    });
  }

  const orders = [];
  for (const [vendorId, lines] of groups.entries()) {
    const id = idFactory();
    if (!id) throw new Error("Could not generate an order ID.");
    const items = {};
    let total = 0;
    for (const line of lines) {
      if (!Number.isFinite(line.unitPrice) || line.unitPrice < 0) {
        throw new Error(`Invalid official price for ${line.foodId}.`);
      }
      items[line.foodId] = line;
      total += line.unitPrice * line.quantity;
    }
    orders.push({
      id,
      vendorId,
      buyerId,
      buyerName: buyerName || "Buyer",
      address: normalizedAddress,
      status: "placed",
      total,
      createdAt: now,
      items,
    });
  }
  return orders;
}

function rangesOverlap(startAt, endAt, otherStartAt, otherEndAt) {
  return startAt < otherEndAt && endAt > otherStartAt;
}

module.exports = {
  ORDER_TRANSITIONS,
  nextOrderStatus,
  normalizeAddress,
  validateStars,
  buildVendorOrders,
  rangesOverlap,
};
