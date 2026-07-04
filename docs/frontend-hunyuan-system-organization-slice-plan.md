# Hunyuan System Organization Slice Plan

## Goal

Turn the current `hunyuan-system` login-plus-bridge shell into the first real
backend-driven module slice, using `组织架构` as the proving ground.

## Why This Slice First

- The backend menu already exposes a coherent `组织架构` group.
- The APIs are stable and easy to verify:
  - `POST /employee/query`
  - `GET /department/listAll`
  - `GET /position/queryList`
  - `GET /role/getAll`
- The slice exercises the full chain we care about:
  - login
  - menu routing
  - page fetch
  - table rendering
  - bridge-to-real-page replacement

## Current State

- Real login is working in `hunyuan-system`.
- Backend menus are loaded from `/login` and `/login/getLoginInfo`.
- Unknown backend pages no longer 404; they land on the module bridge page.
- Guard behavior now clears invalid local auth state and returns to login.

## First Deliverable

Implement a real `员工管理` page at:

- `apps/hunyuan-system/src/views/system/employee/index.vue`

Backed by:

- `apps/hunyuan-system/src/api/system/organization.ts`

## Scope

### In

- Employee list page
- Department filter
- Disabled status filter
- Position / role display
- Pagination
- Empty / loading state
- Keep backend menu path `/organization/employee`

### Out

- Add/edit dialogs
- Batch operations
- Password reset
- Department tree editing
- Role assignment workflows

## Acceptance

- Logging in as `admin / 123456` reaches `hunyuan-system`.
- Clicking `组织架构 -> 员工管理` opens a real page, not the bridge page.
- The page fetches live backend employee data from `1024`.
- Department, role, and position data render from real backend responses.
- `npx vue-tsc --noEmit -p apps/hunyuan-system/tsconfig.json` passes.

## Next After Employee

1. `部门管理`
2. `职务管理`
3. `角色管理`

Keep the bridge page for untouched modules until each route gets a real local
view.
