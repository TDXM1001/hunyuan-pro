# Frontend List and Table Page Standard

## Purpose

This standard defines the default visual baseline for Hunyuan Pro list, search, and table pages. The goal is to keep business pages dense, consistent, and reusable before we optimize individual pages.

## Reference Implementations

- Demo reference: `hunyuan-design/apps/web-ele/src/views/demos/table-test.vue`
- Business reference: `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`
- Drawer-based subordinate-data reference: `hunyuan-design/apps/hunyuan-system/src/views/support/dict/index.vue` + `hunyuan-design/apps/hunyuan-system/src/views/support/dict/components/dict-data-drawer.vue`
- Shared primitives:
  - `hunyuan-design/packages/@vben/art-hooks/src/common/components/art-search-panel/`
  - `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table-panel/`
  - `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table-header/`
  - `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/`

## Layout Contract

- This standard is a default baseline for ordinary business list pages, not a rigid template. If a stronger business prototype, sibling page pattern, or subordinate-detail workflow exists, follow that stronger scene pattern and document why.
- Use `Page` as the outer shell with full available height, `min-h-0`, and hidden outer overflow.
- For business list pages, prefer two major blocks in vertical order: search card first, table card second.
- Keep the table card as the flexible area that consumes the remaining height.
- Split page-only concerns from shared concerns. Search layout, toolbar tools, table settings, and pagination should come from shared primitives first.
- When a row owns subordinate data and the parent list should remain the dominant context, prefer `主表格 + 抽屉/侧滑详情面` over forcing a permanent split-pane page. In that case, keep the list page itself on this standard, and move the subordinate CRUD surface into the drawer.

## Size Baseline

Use these values as the default baseline unless an existing sibling page has a stronger established pattern:

- Outer business page content padding: `12px`
- Main vertical block gap: `16px`
- Left auxiliary tree/list panel width in a two-pane list page: `248px`
- Search card radius: `8px`
- Table card radius: `8px`
- Search card body padding: `16px`
- Search card single-row visual height target: about `92px`
- Search item content widths for compact desktop filters: keyword-like inputs about `216px`, select-like filters about `168px`
- Table card body padding: `16px`
- Compact inner grid gap: `12px`
- Tight inline gap for tags or helper items: `8px`
- Toolbar-to-table spacing inside the table card: `18px`
- Table row visual height target for ordinary data lists: about `58px-62px`, enough for a `28px` avatar without making the page feel inflated.

## Typography Baseline

- Section title: `16px / 24px`, weight `600`
- Secondary description text: `13px / 22px`
- Normal table and form text: `14px`
- Primary inline value text, such as a person name: `14px`, weight `600`

## Page Title Rules

- For ordinary menu-backed management pages, keep the page header extremely quiet. If the active menu, tab, breadcrumb, or route already makes the page context obvious, do not render an extra page title block.
- Do not add explainer copy under a page title just to restate what the menu, route, or table already makes obvious.
- Treat page-level title and description text as exceptions, not defaults. Only add them when the workflow would otherwise be ambiguous without them.
- Do not create hero-style intro blocks for standard CRUD, dictionary, organization, settings, or other routine admin list pages.

## Search Area Rules

- Use `ArtSearchPanel` for ordinary admin search forms instead of hand-rolling a custom action row.
- Search form item spacing should default to `16px` horizontal and `12px` vertical.
- Search inputs, selects, search, reset, and refresh buttons should share the same `32px` control height.
- Search action buttons should use a stable `82px` width by default, so loading states do not resize the query row.
- Search labels, input wrappers, select wrappers, and actions should align to the same vertical center line on desktop.
- On mobile, search actions may wrap to full width, but they should keep the same control height.
- Page-local search item widths may differ by business field, but labels, input wrappers, select wrappers, and action buttons must sit on the same baseline.
- Do not stretch every filter to fill available width on desktop. Compact, repeated widths create a calmer scan line than uneven wide controls.
- When search fields exceed one row on desktop, `ArtSearchPanel` should collapse to one row by default and expose `展开 / 收起`. Do not hand-roll page-local collapse controls.
- When a page only has one natural search row, disable collapse behavior instead of showing an unnecessary `展开 / 收起` toggle.
- Search fields and actions should stay in one natural wrapping flow. The action group should align to the right side of its current row with `margin-left: auto`, but the search area must not be split into a wide left field region and a fixed right action region.

## Toolbar and Button Rules

- Use `ArtTableHeader` for list-page toolbar structure.
- Keep primary business actions on the left and shared table tools on the right.
- Treat the primary left action, such as `新增员工`, as the visual anchor for toolbar sizing.
- Shared tool buttons must align to that anchor and should not visually dominate it.
- Header action buttons should keep the page action baseline, while search action buttons follow the compact `32px` search-control baseline.
- Primary and secondary header buttons should use `18px` horizontal padding.
- Shared table tool buttons should stay square at `32px x 32px`.
- Shared tool button gap should default to `8px`.
- The separation between the left action cluster and the right tool cluster should default to `16px`.
- Shared tool icons should stay at `16px`.
- Tool buttons should keep a quiet neutral background and a `1px` border until hovered or activated.
- Table row actions should stay compact, usually `link` plus `size="small"` at `14px / 22px`, so page-level buttons remain the main emphasis.

## Table Rules

- Use `ArtTablePanel` plus `ArtTable` as the default list-page container.
- Keep selection, index, key business columns, status columns, and action columns deliberate and readable.
- Use overflow tooltips for long text instead of widening the whole table excessively.
- Keep action columns fixed to the right when the page has enough operations to justify it.
- Keep table row actions small and quiet: `ElButton link size="small"` with a `14px / 22px` action text baseline.
- Row action groups must use a page-local `__actions` class, such as `employee-table-panel__actions`, instead of relying on default Element Plus button spacing.
- Row action groups must be `inline-flex`, vertically centered, horizontally centered in the operation column, and use an `8px` action gap.
- Row action buttons must reset extra button chrome inside the action group: `padding: 0` and `.el-button + .el-button { margin-left: 0; }`.
- Operation columns should stay fixed right and centered. Use about `136px` for two compact actions and about `180px` for three compact actions; avoid widening the operation column to solve spacing drift.
- Column and settings popovers should use the shared `ArtTableHeader` compact popover skin: `260px` column panel, `148px` settings panel, `28px` option rows, and `13px` option text.
- Keep shared table section spacing at `8px` unless a page truly needs a looser rhythm.
- Prefer shared pagination and table settings instead of page-local replacements.

## Pagination Rules

- Use shared `ArtTable` pagination instead of page-local pagination markup.
- The default business-list pagination shape is left summary plus right controls: `showTotalSummary: true` with layout `sizes, prev, pager, next, jumper`.
- Keep pagination visible when the current page size is greater than total records. A user still needs page-size and jump controls even when there is only one page.
- Use `showPageCount: true` only when the product explicitly needs total-page text such as `共 128 条 / 13 页`.
- Do not recreate summary alignment with page-scoped CSS. If the split summary/control layout is missing, extend `ArtTable` first and enable it through `pagination-options`.
- The page size select, pager buttons, next/previous buttons, and jump input should keep the shared compact control sizing from `ArtTable`.
- Pagination geometry should stay visually medium and consistent: page size select about `122px`, pager and next/previous buttons `34px`, and jump input about `52px`.

## Style Boundaries

- Use project tokens such as `var(--radius)`, `var(--el-border-color-lighter)`, `var(--el-bg-color)`, and `var(--el-color-primary)`.
- Keep cards and controls calm and dense.
- Avoid oversized buttons, decorative toolbar treatments, or custom icon-button systems for one page.
- If a page needs a different visual weight, adjust shared tokens first before adding page-specific overrides.

## When Not To Use This Pattern

- Do not use this pattern for edit or detail pages. Follow `docs/frontend-edit-detail-page-standard.md`.
- Do not use it for dashboards, landing pages, or highly visual workspaces where tables are not the main task surface.
- Do not replace stronger established sibling patterns without confirming the reuse value first.
- Do not treat capability demos such as `hunyuan-design/apps/web-ele/src/views/demos/form/basic.vue` as business-style references by themselves. They prove the API exists, but business pages still need to match the repo's operational density and page semantics.

## Verification

For any meaningful list, search, or table page change:

1. Read this standard and the direct shared primitive implementation before editing.
2. Prefer the smallest shared-component change that fixes repeated inconsistency.
3. Verify direct consumers when changing shared table or search primitives.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck`.
5. Explain which part of the page was standardized in shared code and which part, if any, remained page-local.
