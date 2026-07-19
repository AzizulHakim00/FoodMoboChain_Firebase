# Security policy

## Supported version

Security fixes are applied to the latest release branch and `main`.

## Protected operations

FoodMoboChain treats the Android application as an untrusted client. These operations run through Firebase Cloud Functions using the Admin SDK:

- administrator bootstrap and custom claims
- vendor approval or rejection
- official-price checkout
- order creation and vendor splitting
- order status transitions
- official-price rental booking and overlap-safe reservations
- review creation
- rating aggregation

Realtime Database and Storage rules deny direct client writes to protected records.

## Secret handling

Never commit:

- Firebase service-account private keys
- Android signing keystores
- keystore passwords
- API secrets
- production environment files

`google-services.json` identifies the Firebase project but is not an Admin SDK credential. Restrict production access with Security Rules, App Check and least-privilege IAM.

## Reporting a vulnerability

Do not publish account data, tokens or exploit details in a public issue. Contact the repository owner privately and include the affected path, reproduction steps and impact.
