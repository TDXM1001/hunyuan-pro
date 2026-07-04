# Frontend Profile Page Standard

## Purpose

This standard defines the rules for personal profile and account settings pages in Hunyuan Pro.

This page family sits between business forms and system pages:

- more personal than an admin list page
- more structured than a generic informational page
- less workflow-heavy than a business edit page

## Parent Standard

This document inherits from:

- `docs/frontend-foundation-style-standard.md`

## Reference Implementations

- `hunyuan-design/apps/hunyuan-system/src/views/_core/profile/index.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/profile/base-setting.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/profile/security-setting.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/profile/password-setting.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/profile/notification-setting.vue`

## Pattern Definition

Profile pages manage the current user's own account context.

Typical content includes:

- personal info
- password changes
- security bindings
- notification preferences

These pages are settings-oriented, not operational record-management pages.

## Composition Rules

- Prefer the shared `Profile` shell from `@vben/common-ui`.
- Use tabs or clearly separated settings groups.
- Keep one user-centric context throughout the page.
- Do not mix profile settings with admin management of other users.

Observed current system baseline:

- shared `Profile` container
- tabbed content
- dedicated subviews for basic info, security, password, and notification settings

## Information Architecture Rules

- Group settings by user mental model, not by backend table structure.
- Keep personal identity settings separate from security settings.
- Keep password change separate from general profile edits.
- Keep notification preferences separate from security preferences.

Rules:

- One tab should correspond to one user concern.
- Do not overload one tab with unrelated setting categories.

## Form and Content Rules

- Profile forms may be richer than auth forms, but should still stay focused.
- Use explicit labels and clear placeholder behavior.
- Validation should remain local and understandable.
- Profile pages may contain descriptive text, but it should stay concise.

Observed current system patterns:

- basic setting form fields
- security status rows with descriptions
- password change form
- notification switch-like preference rows

## Visual Priority Rules

- The user's own identity context should anchor the page.
- The current active tab should be obvious.
- Security-sensitive actions such as password change should remain visually clear but not alarmist.

## Reuse Rules

- Reuse `@vben/common-ui` profile surfaces first.
- Prefer extending profile-oriented subcomponents over building a new page shell.
- If a setting is really an admin business form, move it out of the profile family instead of forcing it into the profile shell.

## What This Standard Covers

This document is normative for:

- profile page family boundary
- tab/group structure
- user-centric settings grouping
- shared profile-shell reuse

This document does not fully define:

- admin list operations
- dialog/drawer business forms
- edit/detail pages for managed records

## Verification

For meaningful profile-page changes:

1. Confirm the page is about the current user rather than a managed business entity.
2. Prefer the shared `Profile` shell and profile subcomponents first.
3. Keep tab boundaries aligned to user-facing concerns.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` after shared frontend changes.
