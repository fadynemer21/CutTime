# Android permissions

CuTime declares only two Android permissions.

## Internet

Manifest value: `android.permission.INTERNET`

Why it is needed:

- Firebase Authentication registration, login, password reset, and logout
  lifecycle.
- Firestore barber catalog, services, availability, appointments, ratings,
  notification inbox, settings, and device registration.
- Firebase Storage gallery upload and download.
- Firebase Cloud Messaging token registration and message delivery.
- Coil loading of Firebase gallery URLs.

Internet is a normal permission and does not show a runtime permission dialog.
The application disables cleartext HTTP; app network traffic must use secure
transport.

## Notifications

Manifest value: `android.permission.POST_NOTIFICATIONS`

Why it is needed on Android 13 and newer:

- appointment confirmations and changes;
- two-hour and 30-minute appointment reminders;
- relevant account/service updates.

The user can deny this permission and continue using account, discovery,
booking, appointment, rating, and in-app inbox features. Notification settings
allow push and categories to be disabled. A denied permission must not trigger
repeated background retries.

## Permissions intentionally not requested

- No camera permission: gallery images use Android's system picker.
- No broad photo/media permission: the picker grants access only to the chosen
  item.
- No location permission: nearby/distance functionality is not implemented.
- No contacts, phone, SMS, microphone, calendar, or Bluetooth permission.
- No exact-alarm permission: WorkManager handles best-effort reminder timing.
- No storage filesystem permission.

## Device test

Test a fresh install on Android 13+:

1. Reach the notification explanation/settings path.
2. Decline permission.
3. Confirm the app remains usable and the in-app inbox still works.
4. Enable permission later from Android Settings.
5. Confirm both reminder notification types display and open the correct
   appointment.

