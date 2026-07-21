# Security policy

## Supported version

Security fixes are applied to the latest release branch and `main`.

## Spark Edition protections

FoodMoboChain treats Android input as untrusted. Realtime Database Security Rules enforce:

- verified-email administrator access
- immutable user identity, role and account status for normal users
- administrator-controlled vendor approval
- approved-vendor-only menu publishing
- cart and order prices matching official menu records
- vendor-specific canonical orders
- immutable submitted order items and buyer information
- permitted order-status transitions by the owning vendor
- delivered-order-only reviews with one deterministic record per participant
- official rental rates and per-day reservation ownership
- authenticated tutorial, newsfeed and reporting operations

App Check adds application-attestation protection. It does not replace Authentication or Security Rules.

## Architecture boundary

Spark Edition intentionally does not use Cloud Functions or Firebase Storage. Public links replace uploads. This means sensitive private documents should not be collected until a private storage and trusted review backend are introduced.

Cash on delivery is used. The app does not collect or store payment-card data.

## Secret handling

Never commit or share:

- Firebase service-account private keys
- Android signing keystores
- keystore passwords
- API secrets
- private identity documents
- production environment files

`google-services.json` identifies the Firebase application but is not an Admin SDK credential. Access is protected by Security Rules, App Check and least-privilege IAM.

Any service-account key accidentally pasted into chat, an issue or source code must be revoked immediately and replaced.

## Reporting a vulnerability

Do not publish account data, tokens or exploit details in a public issue. Contact the repository owner privately with the affected path, reproduction steps and expected impact.
