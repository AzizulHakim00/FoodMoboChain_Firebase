# Firebase production setup

FoodMoboChain connects to the Firebase project identified by `app/google-services.json`. The current configuration targets `foodmobochaindb` and the Android package `com.example.foodmobochain`.

## 1. Required Firebase products

Enable these products in the Firebase Console:

1. Authentication → Email/Password
2. Realtime Database
3. Cloud Storage
4. Cloud Functions
5. App Check

Cloud Functions deployment requires a Firebase project on the Blaze plan.

## 2. Install the Firebase CLI

```bash
npm install -g firebase-tools
firebase login
firebase use foodmobochaindb
```

Run the following commands from the project root.

## 3. Install and test the trusted backend

```bash
npm install --prefix functions --no-audit --no-fund
npm test --prefix functions
```

The backend performs trusted checkout, vendor-specific order creation, controlled order status transitions, rental reservations, vendor approval, review creation and rating aggregation.

## 4. Configure the administrator

The administrator email is no longer hardcoded in the Android application or Security Rules. Configure it as a server-side Functions parameter during deployment:

```bash
firebase deploy --only functions
```

When prompted for `ADMIN_EMAIL`, enter the verified administrator address. After deployment, sign out and sign in again with that address. The `bootstrapAdmin` callable function assigns the secure custom claim.

## 5. Safe deployment order

Deploy in this order so the Android client is never left without its trusted backend:

```bash
firebase deploy --only functions
firebase deploy --only database,storage
```

Complete the verification checklist in GitHub issue #3. Only after the backend and rules are confirmed should the **Build and publish Android APK** workflow be run manually to create the `v1.1.0` prerelease.

## 6. App Check

1. Firebase Console → App Check → register the Android app.
2. Select Play Integrity for production.
3. For debug builds, run the app once and copy the App Check debug token from Logcat.
4. Add that token in Firebase Console → App Check → Apps → Manage debug tokens.
5. Test Authentication, Database, Storage and Functions before enabling enforcement.
6. Enable enforcement gradually after the debug and production builds are confirmed.

## 7. Create accounts and roles

- Buyer and Student accounts become active after registration and email verification.
- Vendor accounts remain pending.
- An administrator approves or rejects vendors through the Admin screen.
- Approval is handled by Cloud Functions and assigns the vendor custom claim.
- Vendors should sign out and sign in again after approval to refresh their ID token.

## 8. Run in Android Studio

1. Open the folder containing `settings.gradle.kts`.
2. Use Android Studio's bundled JDK 17.
3. Install Android SDK 36 when prompted.
4. Sync Gradle.
5. Run on API 24 or newer.

Do not commit `local.properties`, service-account JSON files, upload keystores or signing passwords.
