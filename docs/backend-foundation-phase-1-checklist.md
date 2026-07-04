# Foundation Phase 1 Checklist

## Scope
- `system`
- `file`
- `sms`
- `message`
- `mail`
- shared platform rules

## Status Legend
- `ready`: already usable
- `needs_hardening`: usable but needs production hardening
- `missing`: not yet implemented
- `later`: useful, but not part of phase 1

## Module State

| Module | State | Notes |
| --- | --- | --- |
| system | needs_hardening | Core user, role, menu, department, position, login, and permission flows exist. |
| file | needs_hardening | Upload, storage mode switch, URL resolution, and query exist; consistency and compensation need work. |
| sms | needs_hardening | Template table, template admin CRUD, send log table, provider adapter, mock provider, cache idempotency, per-phone/template rate limit, provider exception/null response handling, masked mock logs, admin send-log query, permission SQL, and provider failure unit tests exist; real provider adapters, callbacks, retry policy, visible frontend pages, and template approval/versioning still need work. |
| message | needs_hardening | Station message flow exists; delivery semantics and duplication control need tightening. |
| mail | needs_hardening | Template-driven mail exists; versioning and operational guardrails are still light. |
| captcha | ready | Already present as a support capability. |
| config | ready | Already present as a support capability. |
| dict | ready | Already present as a support capability. |
| operatelog | ready | Already present as a support capability. |
| loginlog | ready | Already present as a support capability. |
| repeatsubmit | ready | Already present as a support capability. |
| redis | ready | Already present as a support capability. |
| heartbeat | later | Useful, but not the first phase focus. |
| helpdoc | later | Useful, but not the first phase focus. |
| datatracer | later | Useful, but not the first phase focus. |

## Phase 1 Tasks
1. Define the hard boundary between foundation and business.
2. Hardening `file` around consistency and storage semantics.
3. Harden `sms` with real provider adapters, callbacks, retry policy, visible frontend pages, and template governance.
4. Tighten message and mail delivery rules.
5. Standardize platform error, permission, and audit behavior.

## Phase 1 Done When
- The foundation boundary is written down and accepted.
- `file` has a clear minimal reliability path.
- `sms` has a concrete implementation path beyond the current template-driven guarded and tested module.
- Shared platform rules are explicit and reusable.
