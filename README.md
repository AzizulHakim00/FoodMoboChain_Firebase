# FoodMoboChain Android App

FoodMoboChain is a Java/XML Android application based on the supplied SRS and the green/cream Food Mobo Chain design screens. It uses Firebase Authentication, Realtime Database, and Storage.

## Implemented roles

- Buyer: browse food, manage bag, place orders, track delivery, review vendors.
- Vendor: submit an application, manage menu after approval, receive and update orders.
- Student: use ordering, rentals, training, newsfeed, and profile features.
- Admin: fixed verified address `mdomor01815@gmail.com`; approve vendors, moderate reports, and seed demonstration content.

## Implemented SRS features

- Email/password registration, verification, password reset, and 15-minute inactivity timeout.
- Role-specific dashboard and vendor approval state.
- Digital menu CRUD, categories, availability, search, and pricing.
- Firebase bag and multi-vendor order records.
- Order status workflow and transaction reviews/ratings.
- Food-cart rental requests with a Realtime Database transaction lock.
- Tutorial video upload and playback through Firebase Storage.
- Community newsfeed, post creation, reporting, and admin moderation.
- Editable personal/business profile and document upload.
- Low-dependency Java/XML UI designed for API 24+ devices.

## Important setup

Read `FIREBASE_SETUP.md` before running. Firebase project ownership cannot be changed by editing Java code; the valid `google-services.json` must be downloaded while signed in to the intended Firebase account.

## Academic-version payment note

Orders use cash on delivery. No real payment gateway or card data is collected. Firebase provides authenticated access controls plus encryption in transit and at rest; the project does not claim application-layer end-to-end encryption for payments.

## Download the Android app

The repository automatically builds an installable developer-preview APK from the `main` branch.

- Release page: `https://github.com/AzizulHakim00/FoodMoboChain_Firebase/releases/tag/v1.0.0`
- APK name: `FoodMoboChain-v1.0.0-debug.apk`

The published APK is debug-signed and suitable for testing, demonstrations, and project review. A production Google Play release requires a private signing key, Play Console account, store listing, privacy policy, and production testing.

## Automated validation

GitHub Actions installs Android SDK 36, runs the unit tests, builds the debug APK, stores it as a workflow artifact, and publishes or refreshes the `v1.0.0` prerelease.
