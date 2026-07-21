# FoodMoboChain Android App

FoodMoboChain v1.1.0 Spark Edition is a Java/XML Android marketplace MVP designed to run without a Firebase Blaze plan. It uses Firebase Authentication, Realtime Database, server-enforced Security Rules and App Check.

## Roles

- Buyer: browse food, manage a protected bag, place vendor-specific orders, track delivery and review completed transactions.
- Vendor: apply, publish menu items after approval and move orders through the permitted delivery workflow.
- Student: use ordering, rentals, training, newsfeed and profile features.
- Admin: the verified `mdomor01815@gmail.com` account can approve vendors, moderate reports and create starter content.

## Spark-compatible features

- Email/password authentication, email verification and password reset.
- Menu CRUD, availability, categories, search and official-price validation.
- Canonical orders under Realtime Database, split by vendor before submission.
- Immutable order items with totals calculated from rule-validated unit prices.
- Controlled order progression: placed → accepted → preparing → out for delivery → delivered.
- One deterministic review per order participant after delivery.
- Rental requests with official daily pricing and atomic per-day reservation keys.
- Public HTTPS links for tutorials and verification documents instead of Firebase Storage uploads.
- Community newsfeed, reporting and administrator moderation.
- App Check debug and Play Integrity providers.

## Security model

The Android client is not trusted merely because it contains validation code. Realtime Database Security Rules independently check authenticated identity, verified email, administrator email, approved-vendor status, official menu prices, order ownership, allowed status transitions, delivered-order reviews and rental reservation ownership.

This is a production-oriented MVP for controlled pilots and small deployments. A future high-scale commercial edition should add a paid trusted backend for payment processing, refunds, private document storage, audit pipelines and large-scale operations.

## Firebase setup

Read `FIREBASE_SETUP.md`. The configured project is `foodmobochaindb-c36f5` and the package is `com.example.foodmobochain`.

The Firebase Console must contain a default Realtime Database instance. Create it once under **Build → Realtime Database → Create database → Locked mode**, then run the manual **Deploy Spark Realtime Database rules** workflow. No Cloud Functions or Firebase Storage deployment is used.

## APK

The release workflow validates the Security Rules in the Firebase Emulator Suite, runs Android tests, builds the debug APK and publishes:

- Tag: `v1.1.0-spark`
- APK: `FoodMoboChain-v1.1.0-Spark-debug.apk`

The APK is debug-signed and suitable for testing, academic evaluation and controlled pilot deployment. Cash on delivery is used; no card information is collected.
