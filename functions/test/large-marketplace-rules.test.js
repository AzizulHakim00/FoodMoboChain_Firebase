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

let env;
const ADMIN_EMAIL = "mdomor01815@gmail.com";

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-foodmobo-large",
    database: {
      rules: fs.readFileSync(path.join(__dirname, "../../firebase-database-rules.json"), "utf8"),
    },
  });
});

after(async () => {
  if (env) await env.cleanup();
});

function profile(uid, email, role, status) {
  return {uid, name: uid, email, role, status, rating: 0, createdAt: 1};
}

function authDb(uid, email, verified = true) {
  return env.authenticatedContext(uid, {email, email_verified: verified}).database();
}

function food(id, stockCount = 5) {
  return {
    id,
    vendorId: "vendor-large",
    vendorName: "Large Store",
    storeId: "vendor-large",
    name: "Marketplace Meal",
    category: "Combo Meals",
    description: "A complete marketplace meal for rules validation",
    imageUrl: "https://example.com/meal.jpg",
    tags: "combo,meal",
    price: 250,
    regularPrice: 290,
    rating: 0,
    deliveryFee: 30,
    discountPercent: 14,
    preparationMinutes: 25,
    stockCount,
    spicyLevel: 1,
    vegetarian: false,
    available: true,
    featured: false,
    createdAt: 1,
  };
}

function cartLine(quantity = 1) {
  return {
    foodId: "large-food",
    vendorId: "vendor-large",
    vendorName: "Large Store",
    storeId: "vendor-large",
    name: "Marketplace Meal",
    imageUrl: "https://example.com/meal.jpg",
    unitPrice: 250,
    quantity,
  };
}

function preparingOrder() {
  return {
    id: "large-order",
    buyerId: "buyer-large",
    vendorId: "vendor-large",
    storeId: "vendor-large",
    storeName: "Large Store",
    buyerName: "buyer-large",
    address: "House 1, Road 2, Dhaka",
    deliveryNote: "Call at the gate",
    paymentMethod: "cash_on_delivery",
    status: "preparing",
    createdAt: 1000,
    updatedAt: 2000,
    items: {"large-food": cartLine(2)},
  };
}

beforeEach(async () => {
  await env.clearDatabase();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.database();
    await set(ref(db, "users/vendor-large"),
        profile("vendor-large", "vendor-large@example.com", "vendor", "approved"));
    await set(ref(db, "users/buyer-large"),
        profile("buyer-large", "buyer-large@example.com", "buyer", "active"));
    await set(ref(db, "users/admin-large"),
        profile("admin-large", ADMIN_EMAIL, "admin", "active"));
    await set(ref(db, "foods/large-food"), food("large-food"));
  });
});

test("approved vendors can create only their own storefront", async () => {
  const vendor = authDb("vendor-large", "vendor-large@example.com");
  const store = {
    id: "vendor-large",
    ownerId: "vendor-large",
    name: "Large Store",
    cuisine: "Bangladeshi",
    description: "Verified local marketplace store",
    location: "Dhaka",
    rating: 4.5,
    deliveryFee: 30,
    minimumOrder: 100,
    preparationMinutes: 25,
    verified: true,
    open: true,
    featured: false,
    createdAt: 1000,
  };
  await assertSucceeds(set(ref(vendor, "stores/vendor-large"), store));
  await assertFails(set(ref(vendor, "stores/not-owned"), {
    ...store,
    id: "not-owned",
    ownerId: "another-user",
  }));
});

test("cart quantities cannot exceed official stock", async () => {
  const buyer = authDb("buyer-large", "buyer-large@example.com");
  await assertSucceeds(set(ref(buyer, "carts/buyer-large/large-food"), cartLine(5)));
  await assertFails(set(ref(buyer, "carts/buyer-large/large-food"), cartLine(6)));
});

test("users can manage only their own saved addresses", async () => {
  const buyer = authDb("buyer-large", "buyer-large@example.com");
  const address = {
    id: "home",
    userId: "buyer-large",
    label: "Home",
    recipientName: "Buyer Large",
    contactNumber: "01700000000",
    line1: "House 1, Road 2",
    area: "Dhanmondi",
    city: "Dhaka",
    deliveryNote: "Call at the gate",
    defaultAddress: true,
    createdAt: 1000,
    updatedAt: 1000,
  };
  await assertSucceeds(set(ref(buyer, "addresses/buyer-large/home"), address));
  await assertFails(set(ref(buyer, "addresses/another-user/home"), {
    ...address,
    userId: "another-user",
  }));
});

test("users create support tickets and only admin can reply", async () => {
  const buyer = authDb("buyer-large", "buyer-large@example.com");
  const ticket = {
    id: "ticket-1",
    userId: "buyer-large",
    userName: "buyer-large",
    category: "Order",
    subject: "Order delivery delay",
    message: "My order has not moved for a long time.",
    status: "open",
    adminReply: "",
    createdAt: 1000,
    updatedAt: 1000,
  };
  await assertSucceeds(set(ref(buyer, "supportTickets/ticket-1"), ticket));
  await assertFails(update(ref(buyer, "supportTickets/ticket-1"), {
    status: "resolved",
    adminReply: "Resolved by buyer",
    updatedAt: 2000,
  }));

  const admin = authDb("admin-large", ADMIN_EMAIL);
  await assertSucceeds(update(ref(admin, "supportTickets/ticket-1"), {
    status: "in_progress",
    adminReply: "We are checking the store update.",
    updatedAt: 2000,
  }));
  await assertSucceeds(get(query(ref(admin, "supportTickets"),
      orderByChild("userId"), equalTo("buyer-large"))));
});

test("vendor can progress preparing order through packed and dispatch", async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), "orders/large-order"), preparingOrder());
  });
  const vendor = authDb("vendor-large", "vendor-large@example.com");
  await assertSucceeds(update(ref(vendor, "orders/large-order"), {
    status: "packed",
    updatedAt: 3000,
  }));
  await assertSucceeds(update(ref(vendor, "orders/large-order"), {
    status: "out_for_delivery",
    updatedAt: 4000,
  }));
  await assertFails(update(ref(vendor, "orders/large-order"), {
    status: "accepted",
    updatedAt: 5000,
  }));
});
