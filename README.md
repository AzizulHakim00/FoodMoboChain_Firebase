# FoodMoboChain Android Marketplace

FoodMoboChain v1.2.0 is a Java/XML Android food marketplace designed around Bangladesh's food-cart and youth-entrepreneur ecosystem. It combines a modern delivery-app customer experience with vendor operations, training, cart rentals and Firebase Spark-compatible security.

## Marketplace experience

- Foodpanda-inspired home with delivery location, promotion banner, categories and service shortcuts.
- Search, category filtering, rating/price/preparation sorting and professional food imagery.
- Detailed food screens with price, rating, preparation time, delivery information and quantity controls.
- Persistent favourites and complete vendor storefronts.
- Protected bag and vendor-specific checkout using official Firebase menu prices.
- Visual live order tracking: placed → accepted → preparing → out for delivery → delivered.
- Unified notifications for order progress and moderated community announcements.

## Roles

- **Buyer:** discover food, save favourites, manage a protected bag, place orders, track delivery and review completed transactions.
- **Vendor:** apply for approval, operate a seller centre, publish image-rich menu listings and progress customer orders.
- **Student:** use ordering, food-cart rentals, training, community news and profile tools.
- **Admin:** the verified `mdomor01815@gmail.com` account approves vendors, moderates reports and creates the starter marketplace.

## Enterprise starter catalogue

The administrator can create or refresh a complete demonstration marketplace containing popular meals, street foods, snacks, drinks and desserts, realistic preparation/delivery information, rental carts and announcement content. Food imagery uses remote public HTTPS media with an in-app memory cache and a local fallback illustration, so the app stays lightweight and does not require Firebase Storage.

## Spark-compatible security

- Email/password authentication, email verification and password reset.
- Firebase App Check providers for debug and Play Integrity builds.
- Realtime Database Security Rules verify identity, approved-vendor status, ownership and immutable fields.
- Official-price validation for cart lines and order items.
- Vendor-specific orders with restricted status progression.
- One deterministic review per participant after delivery.
- Atomic per-day food-cart rental reservations to prevent duplicate booking.
- Public HTTPS links for images, tutorials and verification documents instead of Blaze-only Storage.

The Android client is treated as untrusted. Sensitive marketplace rules are independently checked by Firebase Realtime Database Security Rules.

## Firebase setup

Read `FIREBASE_SETUP.md`. The configured Firebase project is `foodmobochaindb-c36f5`, the Android package is `com.example.foodmobochain`, and the explicit Singapore database endpoint is configured in `FirebaseService`.

The project uses Firebase Authentication, Realtime Database and App Check without Cloud Functions or Firebase Storage. Keep App Check enforcement disabled for the direct-install debug APK until its debug token has been registered.

## Release

The GitHub Actions release workflow validates the Security Rules in the Firebase Emulator Suite, runs Android tests, builds the APK and publishes:

- Tag: `v1.2.0-enterprise`
- APK: `FoodMoboChain-v1.2.0-Enterprise-debug.apk`

The APK is debug-signed for direct installation and controlled real-user testing. Cash on delivery is used; the app does not collect payment-card information.
