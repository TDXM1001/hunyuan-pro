# BPM Runtime Shell Routing Design

## Goal

Make the employee-side BPM runtime pages render inside the normal Hunyuan admin shell with the left navigation and top tab bar, instead of opening as full-page bare routes.

## Current Problem

- The employee runtime routes are currently declared in [hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts](E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts).
- Static top-level routes bypass the normal backend-generated menu route chain that renders inside `BasicLayout`.
- As a result, `/system/bpm/runtime/startable-list`, `/my-instance-list`, `/my-todo-list`, `/my-done-list`, and `/start-form` can render as standalone pages instead of shell pages.

## Approved Direction

Use the backend menu route chain for the four menu-backed employee list pages, and keep only `start-form` as a hidden shell-internal route.

### Menu-backed pages

The following pages should no longer be owned by static bare routes:

- `/system/bpm/runtime/startable-list`
- `/system/bpm/runtime/my-instance-list`
- `/system/bpm/runtime/my-todo-list`
- `/system/bpm/runtime/my-done-list`

These already have backend menu SQL contracts in [数据库SQL脚本/mysql/sql-update-log/v3.35.0.sql](E:/my-project/hunyuan-pro/数据库SQL脚本/mysql/sql-update-log/v3.35.0.sql), so they should render through the backend-access route pipeline under the normal shell.

### Hidden shell page

`/system/bpm/runtime/start-form` should remain available as a route, but it should render inside `BasicLayout` as a hidden business page:

- keep `hideInMenu: true`
- do not hide it from tabs
- preserve its current route name `SystemBpmRuntimeStartFormRoute`
- keep query-driven behavior for both `definitionId` and `instanceId`

## Route Structure

Keep BPM designer static routes unchanged for now. Narrow this change to the employee runtime flow.

Introduce one shell-wrapped static route group for runtime form navigation only:

- parent route uses `BasicLayout`
- child route points to `/system/bpm/runtime/start-form`
- child route remains hidden from the menu

This keeps the form page inside the shell without duplicating the four menu-backed list routes.

## Success Criteria

- Navigating to the four employee runtime list pages from the menu shows the normal admin shell.
- Navigating from `startable-list` to `start-form` stays inside the same shell and opens as a hidden business tab/page.
- Navigating from `my-instance-list` to `start-form` for resubmit also stays inside the shell.
- No duplicate static route ownership remains for the four menu-backed runtime list pages.
- Route contract tests, frontend typecheck, and live browser verification all pass.
