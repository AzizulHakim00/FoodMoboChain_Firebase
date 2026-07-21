# FoodMoboChain Android Marketplace

FoodMoboChain v1.3.0 Large Marketplace is a Java/XML Android food ecosystem for Bangladesh. It combines multi-store ordering, youth entrepreneurship, food-cart rentals, training and community support while remaining compatible with the Firebase Spark plan.

## Large marketplace data

The verified administrator can safely create or refresh a repeatable starter marketplace without deleting real users or vendor records:

- 12 marketplace stores
- 72 food items with authoritative prices, regular prices, stock, tags, ratings and preparation times
- 22 food categories
- 8 active promotional campaigns
- 10 delivery zones
- 12 rentable food carts
- 12 training resources
- 20 community announcements

Public HTTPS media is loaded with an in-memory cache and local fallback artwork, so Firebase Storage is not required.

## Customer experience

- Food-delivery home with stores, campaigns, featured foods and active-order tracking.
- Store directory and complete store-specific menu pages.
- Search by food, store, category, description and tags.
- Sorting by rating, discount, price and preparation speed.
- Detailed food pages with stock, dietary information, spice level, list price and official checkout price.
- Persistent favourites and stock-aware carts.
- Saved home, work and campus delivery addresses.
- Store-split cash-on-delivery checkout with delivery instructions.
- Six-stage tracking: placed → accepted → preparing → packed → out for delivery → delivered.
- Reviews only after completed delivery.
- Notifications, newsfeed and trackable support tickets.

## Vendor and administrator operations

- Approved vendors automatically receive a verified storefront.
- Seller centre supports public food images, official prices, stock, preparation time, delivery fee and availability.
- Vendor analytics show order volume, completion and delivered value.
- Administrator operations include vendor approval, moderation and large-data creation.
- Administrator analytics cover users, stores, foods, orders, rentals and support tickets.
- Support tickets preserve the customer message, administrator reply and status.

## Firebase Spark security

- Email/password Authentication and verified-email access.
- Firebase App Check providers for debug and Play Integrity builds.
- Realtime Database Security Rules independently verify ownership, roles and immutable fields.
- Store and food ownership validation for approved vendors.
- Official-price and stock validation for cart lines and order items.
- Restricted order-status progression including the packed stage.
- Owner-only saved addresses.
- User-created support tickets with administrator-only replies.
- Atomic per-day rental reservations.
- Public HTTPS links instead of Blaze-only Firebase Storage.

The Android client is treated as untrusted. Security-sensitive business rules are enforced by Firebase Realtime Database Rules and tested with the Firebase Emulator Suite.

## Firebase setup

Read `FIREBASE_SETUP.md`. The configured project is `foodmobochaindb-c36f5`, package is `com.example.foodmobochain`, and the Singapore Realtime Database endpoint is configured explicitly in `FirebaseService`.

The project uses Authentication, Realtime Database and App Check without Cloud Functions or Firebase Storage. Keep App Check enforcement disabled for the direct-install debug APK until its debug token is registered.

## Release

GitHub Actions validates all Security Rules tests, runs Android unit tests, builds the debug APK and publishes:

- Tag: `v1.3.0-large-marketplace`
- APK: `FoodMoboChain-v1.3.0-Large-Marketplace-debug.apk`

The APK is debug-signed for direct installation and controlled real-user testing. Cash on delivery is used; no payment-card data is collected.
