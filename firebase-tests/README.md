# Firebase emulator rule tests

These tests exercise Firestore and Storage authorization without touching the
live CuTime project. They cover private user records, immutable roles,
account-deletion requests, barber-owned management data, version-2 split-shift
availability, appointment privacy, cancelled-history hiding, notification
ownership, the fixed 120/30-minute reminder settings, constrained queries,
gallery ownership, file metadata, file types, reads, and deletes.

## Prerequisites

1. Node.js 22.
2. Java 21.
3. pnpm 10 or newer.

Run from this directory:

```powershell
pnpm install --frozen-lockfile
pnpm test
```

The test command starts temporary Firestore and Storage emulators using the
rules in the repository root, runs the files serially to keep their emulator
setup isolated, and stops the emulators. It does not deploy or mutate the live
Firebase project.

Verified locally on 31 July 2026: **21 tests passed**, 0 failed and 0 skipped.
The local bundled runtime was Node 24, while CI deliberately uses the declared
Node 22 runtime.