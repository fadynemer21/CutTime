# CuTime gallery and notifications setup

This guide covers the Firebase Storage gallery, in-app notifications, Firebase
Cloud Messaging (FCM), local reminders, and the trusted Cloud Functions source
added in the second autonomous mega-batch.

The Android implementation compiles and its JVM tests run without deploying
anything. Live image uploads and server-generated notifications begin working
only after the Firebase resources in this repository are enabled and deployed.

## Architecture summary

### Barber gallery

Each barber owns a maximum of 12 gallery images. The original binary is stored
at:

```text
barberGalleries/{barberId}/{imageId}
```

The customer-facing metadata record is stored at:

```text
barberProfiles/{barberId}/gallery/{imageId}
```

Metadata contains:

- `imageId`
- `barberId`
- `storagePath`
- `downloadUrl`
- `caption`
- `sortOrder`
- `contentType`
- `sizeBytes`
- `createdAt`
- `updatedAt`

The app validates the selected content before upload. It accepts JPEG, PNG,
WebP, HEIC, and HEIF images, rejects empty inputs, limits each file to 8 MB,
limits captions to 120 characters, and prevents adding a thirteenth image.
Firebase Storage rules repeat the ownership, MIME type, size, and metadata
checks at the server boundary.

Image replacement is intentionally represented as delete plus upload. Storage
rules deny in-place replacement so an old metadata document cannot silently
point at new binary content with a misleading type or size.

Firestore is the ordering authority. The management screen uses optimistic
reordering for immediate feedback, writes the complete image-ID order in one
batch, and restores the previous order if the batch fails.

### Notification storage

In-app notifications are stored under the recipient:

```text
users/{userId}/notifications/{notificationId}
```

The Android client may read, mark read, and delete its own notifications. It
cannot create notifications. Cloud Functions uses the Admin SDK to create
trusted lifecycle records.

Each notification contains:

- `notificationId`
- `userId`
- `type`
- `title`
- `message`
- optional `appointmentId`
- optional `barberId`
- `isRead`
- `createdAt`
- optional `readAt`

The supported notification types are:

- `APPOINTMENT_BOOKED`
- `APPOINTMENT_CANCELLED`
- `APPOINTMENT_RESCHEDULED`
- `APPOINTMENT_COMPLETED`
- `APPOINTMENT_REMINDER`
- `REVIEW_REQUEST`
- `GENERAL`

### Preferences and device registrations

Preferences are stored at:

```text
users/{userId}/settings/notifications
```

The preference record controls push delivery, appointment updates, reminders,
and review prompts. When reminders are enabled, every eligible appointment uses
the fixed product schedule:

- 2 hours before the appointment
- 30 minutes before the appointment

The two reminders use separate idempotent local work and server event IDs, so
the later reminder never replaces the earlier one.

FCM registrations are stored at:

```text
users/{userId}/devices/{sha256OfFcmToken}
```

Hashing the token for the document ID gives the same installation an idempotent
write target without exposing the raw token in Firestore paths. The raw token
remains inside the protected document because FCM needs it for delivery.

The app registers the current token after an authenticated app launch and when
Firebase refreshes it. It unregisters the current device before logout. Cloud
Functions removes registrations that FCM reports as invalid or unregistered.

### Reminder layers

CuTime has two complementary reminder paths:

1. Android WorkManager schedules an on-device reminder from the live upcoming
   appointment list. This can notify locally when the device is offline after
   the schedule was saved.
2. A scheduled Cloud Function scans upcoming appointments every 15 minutes and
   delivers an FCM reminder according to each recipient's preference.

The server path uses deterministic notification IDs. Function retries and
successive scheduler scans therefore cannot create or push the same lifecycle
event twice.

The local scheduler replaces work by appointment ID whenever the upcoming list
or preference changes. Cancelled, completed, elapsed, or too-near appointments
do not receive future local work.

## Firebase Console prerequisites

Use the same Firebase project referenced by `.firebaserc` and
`app/google-services.json`.

### Enable Firebase Storage

1. Open Firebase Console.
2. Select the CuTime project.
3. Open **Build > Storage**.
4. Create the default Storage bucket.
5. Choose the same general data region used by Firestore when possible.
6. Do not leave temporary open test-mode rules in production.

Creating a bucket is a console operation. The repository cannot create the
bucket by uploading `storage.rules`.

### Confirm Cloud Messaging

Firebase Cloud Messaging is available for Firebase Android apps. Confirm the
Android package is:

```text
com.fadynemer.cutime
```

If the Firebase Android application was recreated, download a fresh
`google-services.json` and replace the existing local file before building.
Do not place service-account private keys in the Android project.

### Cloud Functions billing

Second-generation Firestore triggers and scheduled functions commonly require
the Firebase project to use the Blaze billing plan. Review the Firebase Console
cost information before deployment. The functions are bounded to one region
and the reminder query scans only upcoming appointments in the next 24 hours.

## Local tools needed for deployment

The machine performing deployment needs:

- Node.js 22
- npm
- Firebase CLI
- permission to deploy to the configured Firebase project

This development machine did not have Node.js or npm on `PATH` during the batch,
so function dependency installation, TypeScript compilation, and Node tests
were not run here. No dependency was installed automatically.

After installing Node.js and Firebase CLI yourself, run:

```powershell
cd C:\Users\Fady\Desktop\CutTime\functions
npm install
npm run lint
npm test
```

The `npm test` script compiles TypeScript and runs the Node built-in test suite
for lifecycle content, preference filtering, deterministic delivery IDs,
routes, FCM message construction, and reminder windows.

## Deploy configuration

Review the active Firebase project first:

```powershell
cd C:\Users\Fady\Desktop\CutTime
firebase projects:list
firebase use
```

Deploy rules and indexes:

```powershell
firebase deploy --only firestore:rules,firestore:indexes,storage
```

Deploy functions after their local tests pass:

```powershell
firebase deploy --only functions
```

Or deploy all included Firebase resources together:

```powershell
firebase deploy --only firestore:rules,firestore:indexes,storage,functions
```

None of these commands was run by the autonomous batch.

## Required composite index

The scheduled reminder function queries:

```text
appointments
  where status == UPCOMING
  where startAt >= now
  where startAt <= now + 24 hours
```

`firestore.indexes.json` includes the corresponding `status ASC, startAt ASC`
composite index. Wait for the Firebase Console to report the index as enabled
before expecting scheduled reminders to complete successfully.

Gallery ordering and notification timestamp ordering use single-field indexes,
which Firestore creates automatically unless explicitly exempted.

## Android manual test: gallery

1. Build and run the debug app.
2. Sign in with a Barber account.
3. Open the **Profile** barber tab.
4. Save a valid shop profile if one does not exist.
5. Tap **Manage Gallery**.
6. Tap **Add Image** and choose a phone image.
7. Confirm upload progress reaches completion.
8. Add a caption and confirm the image card updates.
9. Add at least two images.
10. Use **Move earlier** and **Move later**.
11. Leave and reopen the screen; confirm the saved order remains.
12. Delete one image and confirm the warning before removal.
13. Sign in as a Customer.
14. Open the barber from Home.
15. Confirm the real gallery appears below the profile content.
16. Tap a gallery image and confirm the larger preview opens.

Expected Firebase results:

- The binary exists below `barberGalleries/{barberUid}`.
- Its custom metadata contains the matching barber and image IDs.
- A matching Firestore gallery document exists.
- Deleting from the app removes both the Storage object and metadata document.

## Android manual test: notification inbox

1. Sign in as a Customer.
2. Tap the bell on Home.
3. Confirm the empty state appears before any server events.
4. Open notification settings.
5. Allow Android notification permission when prompted on Android 13 or newer.
6. Confirm the settings screen lists both the 2-hour and 30-minute reminders,
   then save.
7. Book an appointment after Cloud Functions is deployed.
8. Confirm a booking notification appears in the inbox.
9. Confirm the Home badge reflects the unread count.
10. Tap the notification.
11. Confirm it is marked read and opens customer appointment details.
12. Return to the inbox and use **Mark all read**.
13. Delete a notification and confirm it disappears.

Repeat as a Barber:

1. Open the bell from Dashboard.
2. Confirm new customer booking notifications appear.
3. Tap one and confirm barber appointment/customer details open.
4. Reschedule or cancel from the Customer account.
5. Confirm the Barber receives the appropriate lifecycle update.
6. Complete an elapsed appointment.
7. Confirm the Customer receives a review request.
8. Submit a review.
9. Confirm the Barber receives a rating notification.

## Android manual test: reminders

For a complete test, use an appointment more than 2 hours in the future.

1. Keep reminders and push enabled.
2. Open My Appointments once so the current schedule is synchronized into
   WorkManager.
3. Background the app.
4. Confirm one local notification appears around 2 hours before the start.
5. Confirm a second local notification appears around 30 minutes before the
   start.
6. Tap either notification and confirm customer appointment details open.
7. After functions are deployed, confirm the server reminders also reach an
   eligible registered device.
8. Verify the inbox contains exactly two server reminder records for the same
   appointment and recipient: one per required lead time.

Android exact alarms are not required. WorkManager provides durable best-effort
background execution, so power management may delay local delivery slightly.
The 15-minute server scheduler also trades exact-to-the-minute delivery for a
bounded and cost-conscious scan.

## Security test matrix

Test with two Barber accounts and two Customer accounts:

- A Customer cannot upload, replace, or delete gallery objects.
- A Barber cannot manage another Barber's gallery.
- A signed-out request cannot read gallery objects or metadata.
- An upload over 8 MB is rejected by both the app and Storage rules.
- Unsupported MIME types are rejected.
- A thirteenth metadata document is prevented by the app. This is an interface
  limit; administrators should monitor direct or legacy writes because Storage
  rules cannot count Firestore subcollection documents.
- A user cannot create an in-app notification from the Android client.
- A user cannot read or change another user's inbox, settings, or devices.
- Notification updates may change only `isRead` and `readAt`.
- A client cannot rewrite notification title, message, target IDs, or type.
- A user can delete only their own inbox records and device records.
- A client cannot write arbitrary files outside the barber gallery root.
- Logging out removes the current token before clearing authentication.

## Operational notes

### Event retry safety

Firestore triggers are configured with retry enabled. Delivery first attempts
to create a deterministic notification document in a transaction. If it already
exists, the function returns without another push. This makes repeat execution
safe after transient infrastructure failures.

### Preference behavior

Disabling a category prevents future in-app and push records from that category.
General account messages are always eligible, but push still obeys the master
push switch.

The current inbox is not retroactively deleted when a category is disabled.
Users retain the ability to mark or delete existing items themselves.

### Data retention

The app reads the 100 newest notifications. A later production-hardening batch
should add a scheduled retention policy or Firestore TTL field if the product
requires automatic deletion of older records.

### Monitoring after deployment

Watch Cloud Functions logs for:

- invalid appointment or rating documents
- FCM delivery failure counts
- stale-token cleanup
- reminder scan counts
- missing composite-index errors

If a notification record appears but no push arrives:

1. Confirm Android notification permission is granted.
2. Confirm `pushEnabled` is true.
3. Confirm a device document exists under the signed-in user.
4. Confirm the token was not registered to a previously signed-in account.
5. Inspect the Cloud Functions delivery result.
6. Open the app inbox; in-app delivery does not depend on Android system
   notification permission.

## Deliberately deferred

This batch does not:

- deploy Firebase resources
- create or change project billing
- install Node.js, npm packages, or Firebase CLI
- add marketing notifications
- add notification sounds chosen by users
- upload barber avatars
- crop or transform gallery images on a server
- create release signing credentials

Those items require either product decisions, external project access, or final
submission work outside this locally verified batch.
