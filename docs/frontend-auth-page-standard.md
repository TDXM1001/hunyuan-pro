# Frontend Authentication Page Standard

## Purpose

This standard defines the default rules for authentication pages in Hunyuan Pro, including:

- login
- code login
- QR login
- register
- forget-password

Authentication is a distinct page family. It should not inherit business list-page density rules directly, even though it still inherits the same foundation tokens.

## Parent Standard

This document inherits from:

- `docs/frontend-foundation-style-standard.md`

## Reference Implementations

- `hunyuan-design/apps/hunyuan-system/src/views/_core/authentication/login.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/authentication/code-login.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/authentication/qrcode-login.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/authentication/register.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/authentication/forget-password.vue`

## Pattern Definition

Authentication pages are entry pages, not operational admin pages.

They should optimize for:

- clarity
- trust
- low cognitive load
- fast completion

They should not optimize for:

- dense toolbars
- multi-panel business actions
- table-like operational rhythm

## Composition Rules

- Prefer the shared authentication shell from `@vben/common-ui`.
- Keep the page visually centered around one primary task.
- Limit optional routes and parallel entry points unless there is a strong business need.
- Keep authentication pages simpler than the rest of the system.

Observed current system baseline:

- `login.vue` uses `AuthenticationLogin`
- optional variants such as code login, QR login, register, and forget-password are intentionally hidden in the current main login surface

## Variant Rules

### Default Login

- Should remain the primary authentication entry.
- Should show one clear primary submit path.
- Should avoid noisy secondary actions by default.

### Optional Entry Variants

- Code login
- QR login
- register
- forget-password

Rules:

- Only expose these when the business actually supports them.
- Hiding unsupported modes is better than showing dead-end options.
- If a mode is disabled in the backend or current rollout, the frontend should hide it rather than visually teasing it.

## Form Rules

- Authentication forms should stay narrow, linear, and single-purpose.
- Each field should have explicit placeholder and validation intent.
- The primary submit action should remain the strongest visual action on the page.
- Field count should stay minimal.

Observed current system baseline:

- account selector
- username
- password
- hidden device field

Rules:

- Do not overload the login page with profile-like fields, role configuration, or onboarding detail.
- If a field is operationally required but not user-facing, keep it hidden as system state rather than visible clutter.

## Visual Priority Rules

- Brand or product identity may appear, but it must not compete with the login task.
- The form is the center of gravity.
- The primary submit button should remain stronger than all secondary links or alternative modes.

## Content Rules

- Keep copy short and procedural.
- Avoid long marketing text, dense introductions, or decorative information cards.
- Error messaging should be direct and task-oriented.

## Control and Rhythm Rules

- Authentication pages still inherit the foundation control-height and radius baselines.
- However, the page composition should feel calmer and more spacious than a list/search/table page.
- Use shared authentication components first; do not reproduce a custom login shell just to tweak a few pixels.

## Integration Rules

- Authentication pages must stay aligned with the actual auth store and backend capability.
- If a login mode is visually available, it should be functionally supported.
- Do not let the UI imply a workflow that the backend does not actually honor.

## What This Standard Covers

This document is normative for:

- auth page family boundaries
- supported vs hidden auth modes
- task focus
- shared-shell reuse
- copy and visual hierarchy

This document does not fully define:

- business-page toolbars
- list-page spacing
- edit/detail form composition

## Verification

For meaningful authentication page changes:

1. Confirm whether the flow belongs to authentication or to user onboarding/profile management.
2. Prefer the shared `@vben/common-ui` auth shell over page-local layout reinvention.
3. Verify that every visible login mode is actually supported by the current system rollout.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` after shared frontend changes.
