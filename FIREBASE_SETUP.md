# Firebase setup for `mdomor01815@gmail.com`

Firebase connects an Android app to a **Firebase project**, not directly to an email address. The email is the owner/admin account used to create and manage that project.

## 1. Create or select the project

1. Sign in to the Firebase Console with `mdomor01815@gmail.com`.
2. Create a project, or open the intended FoodMoboChain project.
3. Add an Android application with this exact package name:

   `com.example.foodmobochain`

4. Download the new `google-services.json`.
5. Replace `app/google-services.json` in this project with the downloaded file.

The included configuration currently names the project `foodmobochaindb`. A `google-services.json` file does not contain the owner's email, so confirm that `foodmobochaindb` is visible while signed in as `mdomor01815@gmail.com`. If it is not visible, replace the file using the steps above.

## 2. Enable Firebase products

In the Firebase Console:

1. Authentication → Sign-in method → enable **Email/Password**.
2. Realtime Database → create a database in the region you prefer.
3. Storage → create the default bucket.
4. Realtime Database → Rules → paste `firebase-database-rules.json` and publish.
5. Storage → Rules → paste `firebase-storage-rules.txt` and publish.

## 3. Create the administrator

1. Run the app and create an account with `mdomor01815@gmail.com`.
2. Open the verification email and verify the address.
3. Sign in again. The app assigns this verified address the `admin` role.
4. Open **Admin control** and tap **Create starter food and rental data**.

Any other address can register as Buyer, Vendor, or Student. Vendors remain pending until this admin approves them.

## 4. Run in Android Studio

1. Open the folder that contains `settings.gradle.kts` (the `FoodMoboChain` folder).
2. Use Android Studio's bundled JDK 17.
3. Install Android SDK 36 if Android Studio asks for it.
4. Allow Gradle Sync to complete.
5. Run the `app` configuration on an emulator or Android device with API 24 or newer.

Do not upload `local.properties`; Android Studio creates it automatically for each computer.
