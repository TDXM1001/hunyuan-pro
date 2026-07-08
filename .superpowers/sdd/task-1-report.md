# Task 1 Report: Lock the Menu Management Contract with Source Tests

## Scope
- Updated only `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`.
- Added the menu management contract constants and source-test assertions required by the brief.

## What Changed
- Added:
  - `menuPagePath = 'apps/hunyuan-system/src/views/system/menu/index.vue'`
  - `menuApiPath = 'apps/hunyuan-system/src/api/system/menu.ts'`
- Added failing contract checks for:
  - menu page existence
  - shared table/search primitives
  - dense page layout without hero/title/description copy
  - backend menu endpoint wiring
  - route/component/permission fields
  - compact row action styling

## Verification
- Ran:
  - `pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom`
- Result:
  - Failed as expected.
  - The new menu page and API paths do not exist yet, so the contract tests fail on `existsSync(...)` and `ENOENT` reads.

## Notes
- No other source files were modified.
- The failure is intentional and matches the task brief: later implementation tasks will make the new contract pass.
