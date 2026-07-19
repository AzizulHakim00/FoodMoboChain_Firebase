"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  nextOrderStatus,
  normalizeAddress,
  validateStars,
  buildVendorOrders,
  rangesOverlap,
} = require("../lib/domain");

test("order status follows the complete delivery workflow", () => {
  assert.equal(nextOrderStatus("placed"), "accepted");
  assert.equal(nextOrderStatus("accepted"), "preparing");
  assert.equal(nextOrderStatus("preparing"), "out_for_delivery");
  assert.equal(nextOrderStatus("out_for_delivery"), "delivered");
  assert.equal(nextOrderStatus("delivered"), null);
});

test("checkout recalculates official prices and splits vendors", () => {
  let counter = 0;
  const orders = buildVendorOrders({
    cart: {
      a: {foodId: "a", quantity: 2, unitPrice: 1},
      b: {foodId: "b", quantity: 1, unitPrice: 1},
    },
    foods: {
      a: {vendorId: "vendor-1", vendorName: "One", name: "Rice", price: 120, available: true},
      b: {vendorId: "vendor-2", vendorName: "Two", name: "Momo", price: 80, available: true},
    },
    buyerId: "buyer-1",
    buyerName: "Buyer",
    address: "  Dhaka   University  ",
    now: 123,
    idFactory: () => `order-${++counter}`,
  });
  assert.equal(orders.length, 2);
  assert.equal(orders[0].total, 240);
  assert.equal(orders[1].total, 80);
  assert.equal(orders[0].address, "Dhaka University");
});

test("invalid ratings and quantities are rejected", () => {
  assert.throws(() => validateStars(0));
  assert.throws(() => validateStars(6));
  assert.equal(validateStars(5), 5);
  assert.throws(() => buildVendorOrders({
    cart: {a: {foodId: "a", quantity: 21}},
    foods: {a: {vendorId: "v", name: "Food", price: 50, available: true}},
    buyerId: "b",
    buyerName: "B",
    address: "Valid address",
    now: 1,
    idFactory: () => "id",
  }));
});

test("address normalization removes unsafe whitespace", () => {
  assert.equal(normalizeAddress("  Road  1\nDhaka  "), "Road 1 Dhaka");
});


test("rental overlap detection allows adjacent bookings only", () => {
  assert.equal(rangesOverlap(100, 200, 150, 250), true);
  assert.equal(rangesOverlap(100, 200, 200, 300), false);
  assert.equal(rangesOverlap(200, 300, 100, 200), false);
});
