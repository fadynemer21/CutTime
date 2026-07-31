# CuTime account deletion runbook

CuTime provides an in-app request flow that works with Firebase Authentication
and Cloud Firestore on the Spark plan. A signed-in user opens Profile, chooses
**Request account deletion**, confirms the warning, and creates the protected
document `accountDeletionRequests/{uid}`.

## Request security

- The document ID must equal the authenticated Firebase UID.
- Firestore rules copy-proof the UID, Authentication email, and stored role.
- Only the owner can create and read the request.
- Mobile clients cannot change processing status or delete the request.
- Repeated submissions are blocked in the UI and rejected as an update by
  Firestore rules.

## Manual processing procedure

Until a trusted administrative backend is enabled, the Firebase project owner
processes requests manually:

1. Open Firestore and locate `accountDeletionRequests`.
2. Record the request UID, email, role, and request timestamp in the private
   deletion log. Do not copy this information into Git or public issues.
3. Search Authentication for the same UID/email and verify that they match.
4. Review future appointments. Cancel or reassign them according to the
   published customer policy before removing the account.
5. Remove personal/operational documents according to the final retention
   policy. Candidate paths include:
   - `users/{uid}` and its `notifications`, `settings`, and `devices` children;
   - customer appointments and booking-slot locks;
   - for Barber accounts, `barberProfiles/{uid}`, services, availability,
     appointments, reviews/aggregates, and gallery metadata;
   - ratings/reviews authored by the account, subject to the audit policy.
6. Delete the Firebase Authentication user only after dependent cleanup is
   complete. Deleting Authentication first removes the UID's ability to read
   request status and may make verification harder.
7. Delete or archive the request document according to the deletion-log policy.
8. Send completion confirmation to the verified account email if the policy
   permits it.

## Required publisher decisions

Before public release, the publisher must set:

- maximum response/completion time;
- which appointment and rating records are retained or anonymized;
- how open appointments are handled;
- legal/audit exceptions;
- completion-contact wording;
- the public HTTPS deletion-information/request URL required by the store.

The in-app request flow is implemented, and `docs/account-deletion.html` is ready to host. Before submission, the publisher must replace `[SUPPORT_EMAIL]`, confirm the retention wording, and publish that file at a durable public HTTPS URL.
