# Firebase Spark setup

FoodMoboChain v1.3 uses Firebase project `foodmobochaindb-c36f5`, Android package `com.example.foodmobochain`, and the Singapore Realtime Database endpoint.

## Firebase products

The application uses Email/Password Authentication, Realtime Database and App Check. Cloud Functions and Firebase Storage are not required. Public HTTPS media links and cash on delivery keep the project compatible with the Spark plan.

## Deploy Security Rules

The GitHub Actions workflow **Deploy Spark Realtime Database rules** runs automatically when the rules file is merged into `main`. It may also be started manually from the repository Actions page.

Local validation:

```bash
npm install --prefix functions --no-audit --no-fund
./functions/node_modules/.bin/firebase emulators:exec \
  --only database \
  "npm --prefix functions test" \
  --project demo-foodmobo
```

Local deployment:

```bash
firebase login
firebase use foodmobochaindb-c36f5
firebase deploy --only database
```

## Administrator

The verified Authentication account `mdomor01815@gmail.com` is the administrator. Security Rules independently verify the email and verified-email token before allowing administrator operations.

Keep the administrator email synchronized in `FirebaseService.ADMIN_EMAIL` and `firebase-database-rules.json`.

## Create the large starter marketplace

After installing v1.3:

1. Sign in with the verified administrator account.
2. Open **Business workspace → Admin operations**.
3. Tap **Create or refresh enterprise starter data**.
4. Confirm creation.

The repeatable seed creates 12 stores, 72 foods, 22 categories, 8 campaigns, 10 delivery zones, 12 rental carts, 12 training resources and 20 community posts. Real users, real vendor foods, orders, reviews, addresses and support tickets are not removed.

## Vendor storefront workflow

1. A vendor registers and verifies the email.
2. The administrator approves the pending application.
3. Opening Seller Centre creates the vendor storefront under `stores/{uid}`.
4. Published foods use the vendor UID as both owner and storefront ID.
5. Security Rules prevent publishing into another vendor's store.

## App Check

Use Play Integrity for production-signed builds. For the direct-install debug APK, register its debug token before enabling enforcement. Keep enforcement disabled until login, saved addresses, support tickets, ordering and rentals have been tested.

## Public media

Use public `https://` links for food images, store banners and tutorials. Do not expose identity documents or private files through public links.

## Android Studio

Use JDK 17, Android SDK 36 and Android API 24 or newer. Never commit credentials, signing files, passwords or private user documents.
