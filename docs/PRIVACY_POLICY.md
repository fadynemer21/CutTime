# CuTime privacy policy — submission draft

Last draft update: 31 July 2026

> Replace every bracketed owner/contact item and have this document reviewed
> against the final deployed product before publishing it in a store listing.

CuTime (“the app”, “we”, “us”) helps customers discover barber shops and book
appointments, and helps barbers manage their public shop information and
schedule. This policy explains the data used by the current Android
implementation.

## Data the app handles

### Account information

When a person registers, the app uses an email address, display name, password,
and selected Customer or Barber role. Firebase Authentication processes account
credentials. Cloud Firestore stores the account UID, email, display name, role,
and account timestamps.

Passwords are handled by Firebase Authentication and are not stored in CuTime
Firestore documents.

### Barber public content

A Barber account can provide a shop name, description, services, prices,
durations, working hours, holiday dates, and gallery photos or captions.
Customers signed into CuTime can view this public shop content.

### Appointment information

An appointment contains customer and barber identifiers, the customer's name
and email, shop and service details, price, duration, date, time, status, slot
locks, and lifecycle timestamps. The customer and owning barber can view the
appointment. The email is retained for operational identity but the barber UI
is designed to show the customer name as the primary identity.

### Ratings and reviews

After a completed appointment, a customer may submit a one-to-five-star rating
and optional review. Signed-in users may view barber ratings and review text.

### Gallery files

When gallery support is enabled, the app uploads a barber-selected image and
associated type, size, caption, order, and ownership metadata to Firebase
Storage and Firestore. The Android system picker is used; the app does not
request broad photo-library access.

### Notifications and device information

The app may store a Firebase Cloud Messaging token, hashed device document ID,
app version, device manufacturer/model string, notification preferences, and
notification delivery/read records. This data is used for appointment and
account notifications. On-device WorkManager also schedules appointment
reminders.

### Diagnostic information

Firebase and Google Play infrastructure may process technical diagnostics
according to the services enabled in the final Firebase and Play Console
configuration. The current app does not intentionally add an analytics SDK or
advertising SDK. Confirm the final dependency and console configuration before
publication.

## How data is used

Data is used to:

- create and secure accounts;
- restore signed-in sessions;
- show eligible barber shops and services;
- calculate availability and prevent double-booking;
- create, cancel, reschedule, complete, and display appointments;
- display ratings and aggregate barber scores;
- store and display barber gallery content;
- deliver in-app, push, and local appointment reminders;
- enforce role and ownership rules;
- diagnose failed operations and maintain service security.

CuTime does not use the current implementation to sell personal data or serve
third-party advertising.

## Legal basis and consent

The final publisher is responsible for choosing and documenting the applicable
legal basis in every launch country. Where consent is required, notification
permission is requested through Android and can be declined. Account data is
needed to provide authenticated booking services.

## Sharing and processors

The app uses Google Firebase services, including Authentication, Cloud
Firestore, Cloud Storage, Cloud Messaging, and Cloud Functions when enabled.
Google acts as a service provider/processor under the publisher's Firebase
terms and configuration.

Appointment details are shared between the customer who booked and the owning
barber. Public barber content, gallery content, and reviews are visible to
signed-in app users.

## Retention and deletion

- Authentication and user records remain while the account exists.
- Appointment records are retained for customer/barber history and integrity.
  Customers can hide their own cancelled appointments from their history; this
  does not delete the barber's record.
- Notification records can be deleted by their owner. The current app reads the
  newest 100 records.
- Barber gallery objects can be deleted by the owning Barber account.
- Ratings are immutable audit records in the current implementation.

The app includes an authenticated account-deletion request action. Requests are
queued for administrator processing; the account stays active until cleanup is
complete. Before release, define the final retention periods, processing time,
and public HTTPS deletion-information/request page required by the store.

## Security

CuTime uses Firebase Authentication, owner- and role-scoped Firestore/Storage
rules, HTTPS-only network policy, transaction-protected appointment locks, and
an allow-list for notification routes. Android backup is disabled. No system
can guarantee absolute security, and the publisher must monitor Firebase access
and function logs after deployment.

Firebase Storage download-token URLs act as bearer URLs. A person with a copied
valid URL may be able to access the referenced file. Barbers should upload only
work images they are authorized to share publicly with signed-in customers.

## Choices

Users can:

- decline Android notification permission;
- disable push, appointment updates, reminders, or review prompts in app
  settings;
- edit their display name;
- delete individual notification records;
- hide their own cancelled appointments;
- request account/data help from the publisher.
- submit an authenticated account-deletion request from Profile.

## Children

The final publisher must define the product's minimum age and configure the Play
Console target audience accordingly. CuTime is not intentionally designed for
children in its current product specification.

## International processing

Firebase data may be processed in locations determined by the publisher's
Firebase resource regions and Google's infrastructure. The notification
Functions source currently specifies `europe-west1`; confirm all live resource
locations before publication.

## Changes

This policy may be updated when features, legal requirements, or service
providers change. The published policy should show an effective date and a
durable URL.

## Contact

Publisher/legal name: `[OWNER OR COMPANY NAME]`  
Privacy email: `[PRIVACY CONTACT EMAIL]`  
Country/address if required: `[PUBLISHER ADDRESS]`
