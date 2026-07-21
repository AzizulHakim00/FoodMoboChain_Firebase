# Firebase Spark setup

FoodMoboChain connects to Firebase project `foodmobochaindb-c36f5` with Android package `com.example.foodmobochain`.

## 1. Enable free Firebase products

In Firebase Console:

1. Authentication → Sign-in method → enable Email/Password.
2. Build → Realtime Database → Create database.
3. Choose an appropriate nearby region and start in Locked mode.
4. App Check → register the Android app.

Cloud Functions and Firebase Storage are not required by Spark Edition.

## 2. Deploy Realtime Database rules

A fresh Firebase project must have its default Realtime Database created once in the Console before rules can be deployed.

The repository contains the secret `FIREBASE_SERVICE_ACCOUNT_JSON`. After the database exists, open GitHub Actions and run:

**Deploy Spark Realtime Database rules**

Local alternative:

```bash
npm install --prefix functions --no-audit --no-fund
./functions/node_modules/.bin/firebase emulators:exec \
  --only database \
  "npm --prefix functions test" \
  --project demo-foodmobo

firebase login
firebase use foodmobochaindb-c36f5
firebase deploy --only database
```

## 3. Administrator

The verified Firebase Authentication account `mdomor01815@gmail.com` is the administrator. On sign-in, the app synchronizes its profile to role `admin`; Security Rules independently verify the email and `email_verified` token before permitting administrator operations.

Do not change the administrator email in only one place. Keep these synchronized:

- `FirebaseService.ADMIN_EMAIL`
- `firebase-database-rules.json`

## 4. Vendor approval

1. A vendor registers and verifies their email.
2. Their profile starts with status `pending`.
3. The administrator opens Admin control and approves or rejects the application.
4. An approved vendor can publish menu records whose `vendorId` matches their Firebase UID.

## 5. App Check

1. Register `com.example.foodmobochain` in Firebase App Check.
2. Use Play Integrity for production-signed builds.
3. For the debug APK, run once and copy the debug token from Logcat.
4. Add the debug token in Firebase Console before enabling enforcement.
5. Enable enforcement only after Authentication and Realtime Database flows have been tested.

## 6. Public media links

Spark Edition does not upload files to Firebase Storage. Use public `https://` links for:

- tutorial videos
- profile verification documents
- future food/news images

Do not use a public link for sensitive identity documents in a real commercial deployment. A paid private storage and review workflow should be added before collecting sensitive documents.

## 7. Android Studio

1. Open the folder containing `settings.gradle.kts`.
2. Use JDK 17.
3. Install Android SDK 36 when prompted.
4. Sync Gradle and run on API 24 or newer.

Never commit service-account keys, signing keystores, passwords or private user documents.
