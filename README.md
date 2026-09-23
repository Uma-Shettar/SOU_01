# sou

A personal account/profile management app built with a Flutter frontend and a Flask + Appwrite backend. It also has an unrelated SMS-listening feature bundled into the same app (a native Android method channel that starts an SMS receiver on launch — separate from the user-account features, presumably for reading incoming bank SMS, based on a /get-new-message + transaction-parsing endpoint on the backend).

User-facing features (Flutter)

Register a new account
Log in with mobile number + password
Session persists across app restarts (secure storage) — reopening the app should land you straight on your profile, no re-login needed
View and edit your own profile (name, mobile, country code, email, pincode)
Change your password
Delete your own account
Log out

Backend (Flask + Appwrite)

/user/users — register (POST), list all (GET, admin-only), update (PATCH), delete (DELETE)
/user/users/by-id, /user/users/by-mobile, /user/users/me — profile lookups
/user/login — authenticates against Appwrite and returns a session
Every route except register/login requires a valid session (Authorization: Bearer <secret>), and users can only read/edit/delete their own profile — enforced by checking session identity against document ownership
Admin-only access to the full user list, via Appwrite user labels
Appwrite handles the actual auth (accounts, passwords, sessions); a separate Appwrite database collection stores each user's profile fields, linked by auth_user_id

