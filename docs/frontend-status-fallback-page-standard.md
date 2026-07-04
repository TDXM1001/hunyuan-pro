# Frontend Status and Fallback Page Standard

## Purpose

This standard defines the rules for status and fallback pages in Hunyuan Pro, including:

- 403
- 404
- 500
- offline
- coming soon

These pages are not ordinary content pages. Their job is to communicate state clearly and route the user to the next sensible action.

## Parent Standard

This document inherits from:

- `docs/frontend-foundation-style-standard.md`

## Reference Implementations

- `hunyuan-design/apps/hunyuan-system/src/views/_core/fallback/forbidden.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/fallback/not-found.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/fallback/internal-error.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/fallback/offline.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/_core/fallback/coming-soon.vue`

## Pattern Definition

Fallback pages should answer three questions quickly:

1. what happened
2. whether it is dangerous or recoverable
3. what the user should do next

## Composition Rules

- Prefer the shared `Fallback` shell from `@vben/common-ui`.
- Keep the page centered around one state message.
- Avoid mixing fallback pages with normal business content.
- Keep the action model simple.

Observed current system baseline:

- each current fallback page delegates directly to a shared `Fallback` component with a status code or state key

## Status Family Rules

### 403 Forbidden

- Communicate permission limitation clearly.
- Do not suggest the resource is missing.

### 404 Not Found

- Communicate path or resource absence clearly.
- Encourage returning to a known location.

### 500 Internal Error

- Communicate system failure clearly.
- Avoid exposing raw technical detail to ordinary users.

### Offline

- Communicate connectivity loss clearly.
- Emphasize retry or return once connectivity is restored.

### Coming Soon

- Communicate intentional unavailability, not failure.

## Copy Rules

- Copy should be short, direct, and action-oriented.
- Do not overload fallback pages with explanation paragraphs.
- The user should understand the state within seconds.

## Visual Rules

- Fallback pages may be slightly more expressive than dense admin pages, but they must stay system-like.
- The status illustration or emphasis should support the message, not overwhelm it.
- Destructive-looking states should be reserved for real failure or danger.

## Reuse Rules

- Reuse the shared fallback shell first.
- Do not build one-off 404 or 500 layouts per route.
- If a status page needs a custom action or copy, extend the shared fallback usage rather than reinventing the page family.

## What This Standard Covers

This document is normative for:

- fallback page family boundary
- shared fallback-shell reuse
- message clarity
- low-complexity action design

This document does not fully define:

- empty states inside ordinary business tables
- inline form validation states
- dashboard informational cards

Those belong to other standards.

## Verification

For meaningful fallback/status page changes:

1. Confirm the state is truly a standalone fallback page rather than an inline empty/error state.
2. Prefer the shared `Fallback` shell from `@vben/common-ui`.
3. Keep actions and copy short enough that the user can recover quickly.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` after shared frontend changes.
