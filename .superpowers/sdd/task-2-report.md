# Task 2 Report: Add the Menu API Module

## Delivered

- Added `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts`.
- The module exports the requested menu record types, form types, payload helper, and API functions.
- The API methods use `requestClient` from `#/api/request` and target the brief's endpoint paths:
  - `/menu/query`
  - `/menu/tree`
  - `/menu/detail/:menuId`
  - `/menu/add`
  - `/menu/update`
  - `/menu/batchDelete`
  - `/menu/auth/url`

## Verification

- Source contract test:
  - `pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom`
  - Result: failed as expected because `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue` does not exist yet.
  - The API endpoint assertions in the test now point at the new menu module.
- Typecheck:
  - `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`
  - Result: passed.

## Notes

- This task intentionally did not implement the Vue menu page.
- No other business areas were changed.

## Fix

- Aligned `batchDeleteMenus` with the backend `@RequestParam("menuIdList") List<Long>` contract by building a repeated query string with `URLSearchParams`, producing explicit `menuIdList=...` entries.
- Updated `RequestUrlRecord` to the backend shape with `comment`, `name`, and `url`, and removed the unused `method` field.
- Adjusted the organization module source contract test so it checks the new query-string builder and the updated request-url fields.

## Verification

- `pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom`
  - Result: failed as expected because `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue` is still missing.
  - After the API contract update, the remaining failures are the four menu-page existence/content assertions.
- `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`
  - Result: passed.
