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
const {ref, set, update, get} = require("firebase/database");

let testEnv;

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
    await set(ref(db, "users/vendor-1"), {
      uid: "vendor-1",
      name: "Vendor One",
      email: "vendor@example.com",
      role: "vendor",
      status: "approved",
      rating: 0,
      createdAt: 1,
    });
    await set(ref(db, "foods/food-1"), {
      id: "food-1",
      vendorId: "vendor-1",
      vendorName: "Vendor One",
      name: "Rice",
      category: "Rice",
      description: "Fresh rice",
      price: 100,
      available: true,
      createdAt: 1,
    });
  });
});

test("unauthenticated users cannot browse foods", async () => {
  const db = testEnv.unauthenticatedContext().database();
  await assertFails(get(ref(db, "foods")));
});

test("buyers cannot write protected order or rating records", async () => {
  const db = testEnv.authenticatedContext("buyer-1", {
    email: "buyer@example.com",
    email_verified: true,
  }).database();
  await assertFails(set(ref(db, "orders/order-1"), {buyerId: "buyer-1"}));
  await assertFails(set(ref(db, "reviews/review-1"), {authorId: "buyer-1"}));
  await assertFails(set(ref(db, "ratingStats/vendor-1"), {sum: 5, count: 1}));
});

test("a user can create a constrained profile but cannot promote their role", async () => {
  const db = testEnv.authenticatedContext("buyer-1", {
    email: "buyer@example.com",
    email_verified: true,
  }).database();
  await assertSucceeds(set(ref(db, "users/buyer-1"), {
    uid: "buyer-1",
    name: "Buyer One",
    email: "buyer@example.com",
    role: "buyer",
    status: "active",
    rating: 0,
    createdAt: 1,
  }));
  await assertSucceeds(update(ref(db, "users/buyer-1"), {name: "Buyer Updated"}));
  await assertFails(update(ref(db, "users/buyer-1"), {role: "admin"}));
});

test("only a vendor claim can publish food for the same vendor UID", async () => {
  const noClaimDb = testEnv.authenticatedContext("vendor-1", {
    email: "vendor@example.com",
    email_verified: true,
  }).database();
  await assertFails(update(ref(noClaimDb, "foods/food-1"), {price: 110}));

  const vendorDb = testEnv.authenticatedContext("vendor-1", {
    email: "vendor@example.com",
    email_verified: true,
    vendor: true,
    role: "vendor",
  }).database();
  await assertSucceeds(update(ref(vendorDb, "foods/food-1"), {price: 110}));
  await assertFails(set(ref(vendorDb, "foods/food-2"), {
    id: "food-2",
    vendorId: "someone-else",
    vendorName: "Other",
    name: "Momo",
    category: "Snacks",
    description: "Fresh momo",
    price: 90,
    available: true,
    createdAt: 1,
  }));
});

test("cart prices must match the official menu", async () => {
  const db = testEnv.authenticatedContext("buyer-1", {
    email: "buyer@example.com",
    email_verified: true,
  }).database();
  const validLine = {
    foodId: "food-1",
    vendorId: "vendor-1",
    vendorName: "Vendor One",
    name: "Rice",
    unitPrice: 100,
    quantity: 2,
  };
  await assertSucceeds(set(ref(db, "carts/buyer-1/food-1"), validLine));
  await assertFails(set(ref(db, "carts/buyer-1/food-1"), {...validLine, unitPrice: 1}));
});
