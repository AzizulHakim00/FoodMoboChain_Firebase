# FoodMoboChain Android App

> v1.1 security foundation is under review on `agent/v1.1-security-foundation`.

FoodMoboChain is a Java/XML Android application based on the supplied SRS and the green/cream Food Mobo Chain design screens. It uses Firebase Authentication, Realtime Database, and Storage.

## Implemented roles

- Buyer: browse food, manage bag, place orders, track delivery, review vendors.
- Vendor: submit an application, manage menu after approval, receive and update orders.
- Student: use ordering, rentals, training, newsfeed, and profile features.
- Admin: receives a server-issued custom claim from a configured verified account; approves vendors, moderates reports and seeds demonstration content.

## Implemented SRS features

- Email/password registration, verification, password reset, and 15-minute inactivity timeout.
- Role-specific dashboard with server-issued administrator and approved-vendor custom claims.
- Digital menu CRUD, categories, availability, search, and pricing.
- Firebase bag plus trusted server-side checkout that recalculates official prices and creates one order per vendor.
- Server-controlled order status workflow, immutable transaction reviews and automatic rating aggregation.
- Trusted rental booking with official server-side pricing and overlap-safe date reservations.
- Tutorial video upload and playback through Firebase Storage.
- Community newsfeed, post creation, reporting, and admin moderation.
- Editable personal/business profile and document upload.
- Low-dependency Java/XML UI designed for API 24+ devices.

## Security architecture

The Android app is treated as an untrusted client. Cloud Functions handle checkout, vendor approval, status transitions, reviews and rating totals. Realtime Database and Storage rules deny direct client writes to protected records. Firebase App Check is initialized with the debug provider for debug builds and Play Integrity for release builds.

## Important setup

Read `FIREBASE_SETUP.md` before running or deploying. Deploy Functions before the tightened Database and Storage rules. Firebase project ownership cannot be changed by editing Java code; the valid `google-services.json` must be downloaded while signed in to the intended Firebase account.

## Academic-version payment note

Orders use cash on delivery. No real payment gateway or card data is collected. Firebase provides authenticated access controls plus encryption in transit and at rest; the project does not claim application-layer end-to-end encryption for payments.


## Download the Android app

The current public developer-preview APK remains available from the `v1.0.0` GitHub prerelease. After this security pull request is deployed and merged, the next release should be published as `v1.1.0`.

- Current release: `https://github.com/AzizulHakim00/FoodMoboChain_Firebase/releases/tag/v1.0.0`
- Current APK: `FoodMoboChain-v1.0.0-debug.apk`

## Automated validation

Pull requests and security branches run backend unit tests, Firebase Realtime Database Rules tests in the Emulator Suite, Android unit tests and a debug APK build.
