# Google Play Data Safety draft

This is a preparation worksheet, not a final Play Console declaration. Answer
the live form using the final release bundle, enabled Firebase products,
publisher practices, retention policy, privacy policy, and current Google Play
definitions.

## Collection overview

| Data category | Example in CuTime | Collected | Shared externally | Purpose |
|---|---|---:|---:|---|
| Name | Customer full name, barber owner display name | Yes | Service provider; appointment counterpart | Account management, booking |
| Email address | Authentication and appointment identity | Yes | Service provider; owning barber appointment record | Account management, booking |
| User IDs | Firebase UID | Yes | Service provider | Authentication, ownership |
| Photos | Barber gallery selections | Optional | Service provider; signed-in customers | App functionality |
| User-generated content | Shop description, captions, reviews | Optional | Service provider; signed-in users | App functionality |
| App interactions | Appointment and notification lifecycle | Yes | Service provider | App functionality, notifications |
| Device or other IDs | FCM token and hashed device document ID | Yes when notifications enabled | Service provider | Push notifications, fraud/security |
| Diagnostics | Provider/platform diagnostics if enabled | Confirm in consoles | Service provider | Reliability/security |
| Purchase information | None in current app | No | No | Not applicable |
| Precise/approximate location | Not requested or implemented | No | No | Not applicable |
| Contacts | Not requested | No | No | Not applicable |
| Health/fitness | Not requested | No | No | Not applicable |
| Financial/payment information | Service price is booking content, not payment data | No payment data | No | Not applicable |

“Shared externally” must be interpreted using Google's current exceptions. Data
sent only to Firebase as a contracted service provider may be treated
differently from data shared with another user. Verify this in the current Play
Console rather than copying this column directly.

## Required/optional

- Account name, email, UID, and role are required for authenticated booking.
- Appointment details are required when a booking is made.
- Ratings/reviews are optional.
- Barber shop content is required only for Barber accounts that publish a shop.
- Gallery photos and captions are optional.
- Android notification permission and notification preferences are optional.
- FCM device registration is attempted for signed-in users, but visible system
  notification delivery still depends on permission.

## Processing and ephemerality

- Authentication, user, barber, appointment, rating, notification, preference,
  device, and gallery metadata persist in Firebase.
- Gallery files persist in Firebase Storage until the barber deletes them.
- Selected photo URIs are used to upload a file; the app does not request broad
  library access.
- WorkManager input temporarily contains appointment ID and reminder content.
- Search queries are local Compose state and are not intentionally uploaded.

## Security statements to verify

- Data is encrypted in transit by Firebase SDK HTTPS connections.
- Firebase encrypts hosted data at rest under its service controls; verify the
  statement appropriate to the final Firebase agreement.
- Firestore and Storage rules restrict ownership and roles.
- Android backup is disabled.
- Cleartext network traffic is disabled.
- A public process for deletion requests still needs to be defined.

## Account deletion readiness

The release candidate provides an authenticated in-app deletion-request flow.
Before submission:

1. Publish the required public web deletion-information/request mechanism.
2. Define treatment of appointments, ratings, public barber content, gallery
   objects, device tokens, and notification records.
3. Publish the final process in the privacy policy and Play Console.
4. Test deletion using both Customer and Barber accounts.

## Final review questions

- Is Crashlytics, Analytics, Performance Monitoring, App Check, or any SDK
  enabled in the final build or Firebase console?
- Are Functions and Storage deployed and used in production?
- What are the launch countries and minimum age?
- Is review text visible publicly or only to signed-in users?
- How long are appointments and notification records retained?
- Is data used for fraud prevention beyond security rules?
- Is any data exported to support, analytics, or marketing tools?
- Does the privacy policy URL work without signing in?
- Does the deletion URL work without signing in?
