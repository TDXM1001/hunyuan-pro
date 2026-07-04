# Employee Management Recreate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recreate the `员工管理` page to match the provided reference image with high fidelity while staying inside the repo's frontend standards and leaving business behavior unchanged.

**Architecture:** Keep the existing employee management page structure and business API flow, then layer the visual recreation in two tiers. Tier 1 is page-local high-fidelity styling in `employee` view components; tier 2 is a very small shared enhancement in `ArtTable` pagination where the current shared API cannot express the target layout cleanly.

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc

## Global Constraints

- Must comply with `docs/frontend-foundation-style-standard.md`.
- Must comply with `docs/frontend-list-table-page-standard.md`.
- Must comply with `docs/frontend-tree-list-page-standard.md`.
- Must comply with `docs/frontend-dialog-drawer-form-standard.md`.
- Do not add new dependencies.
- Do not change employee-management business workflows, API contracts, or core interaction semantics.
- Prefer page-level styling for the first high-fidelity pass; only extract stable primitives back to shared code.
- Keep browser-level scrolling disabled; inner work surfaces own scrolling.

---

## File Structure

### Shared files

- Modify: `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/index.ts`
  - Add a minimal shared pagination summary mode so the employee page can show `共 X 条` without inventing a page-local fake paginator.
- Modify: `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/style.css`
  - Tune pagination region layout hooks that the employee page can inherit.
- Create: `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts`
  - Add a focused rendering test for the new pagination summary mode.

### Employee page files

- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`
  - Keep the shell structure, but tighten the page-level container rhythm and alignment.
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-org-tree.vue`
  - Recreate the left tree panel to match the reference image.
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue`
  - Recreate the search bar, toolbar, table card, tags, row presentation, and pagination layout.
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-form.vue`
  - Align dialog and success-state styling with the new page standard.

## Task 1: Add a Shared Pagination Summary Mode for High-Fidelity Table Footers

**Files:**
- Create: `E:/my-project/hunyuan-pro/hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/index.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/style.css`

**Interfaces:**
- Consumes: existing `ArtTable` props `pagination` and `paginationOptions`
- Produces:
  - `PaginationOptions['summaryMode']?: 'count' | 'count-and-pages' | 'none'`
  - `PaginationOptions['showPageCount']` remains backward-compatible; employee page will use `summaryMode: 'count'`

- [ ] **Step 1: Write the failing test**

```ts
import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';

import ArtTable from '../index';

describe('ArtTable pagination summary', () => {
  it('renders count-only summary when summaryMode is count', () => {
    const wrapper = mount(ArtTable, {
      props: {
        columns: [{ prop: 'name', label: '姓名' }],
        data: [{ name: '张三' }],
        pagination: {
          current: 1,
          size: 10,
          total: 128,
        },
        paginationOptions: {
          layout: 'sizes, prev, pager, next, jumper',
          summaryMode: 'count',
        },
      },
    });

    expect(wrapper.text()).toContain('共 128 条');
    expect(wrapper.text()).not.toContain('/');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm --dir hunyuan-design exec vitest run packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts --dom`

Expected: FAIL because `summaryMode` is not implemented and the summary text is missing or still tied to `showPageCount`.

- [ ] **Step 3: Write minimal implementation**

```ts
interface PaginationOptions {
  align?: 'center' | 'left' | 'right'
  background?: boolean
  hideOnSinglePage?: boolean
  layout?: string
  pagerCount?: number
  pageSizes?: number[]
  showPageCount?: boolean
  size?: 'default' | 'large' | 'small'
  summaryMode?: 'count' | 'count-and-pages' | 'none'
}

const mergedPaginationOptions = computed(() => ({
  align: 'center' as const,
  background: true,
  hideOnSinglePage: false,
  layout: layout.value,
  pageSizes: [10, 20, 30, 50, 100],
  pagerCount: width.value > 1200 ? 7 : 5,
  showPageCount: false,
  size: 'default' as const,
  summaryMode: 'none' as const,
  ...props.paginationOptions,
}))

const paginationSummaryText = computed(() => {
  if (!props.pagination) return ''
  const totalPages =
    props.pagination.total > 0
      ? Math.ceil(props.pagination.total / props.pagination.size)
      : 0

  if (mergedPaginationOptions.value.summaryMode === 'count') {
    return `共 ${props.pagination.total} 条`
  }

  if (
    mergedPaginationOptions.value.summaryMode === 'count-and-pages' ||
    mergedPaginationOptions.value.showPageCount
  ) {
    return `共 ${props.pagination.total} 条 / ${totalPages} 页`
  }

  return ''
})
```

```ts
const pagination = props.pagination && props.data.length > 0
  ? h('div', { class: ['pagination', 'custom-pagination', mergedPaginationOptions.value.align] }, [
      paginationSummaryText.value
        ? h('span', { class: 'pagination-summary' }, paginationSummaryText.value)
        : null,
      h(ElPagination, {
        ...mergedPaginationOptions.value,
        currentPage: props.pagination.current,
        disabled: props.loading,
        pageSize: props.pagination.size,
        total: props.pagination.total,
        onCurrentChange: (val: number) => {
          emit('pagination:current-change', val)
          scrollToTop()
        },
        onSizeChange: (val: number) => emit('pagination:size-change', val),
      }),
    ])
  : null
```

```css
.art-table .pagination {
  display: flex;
  gap: 12px;
  margin-top: var(--art-table-section-gap);
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.art-table .pagination-summary {
  flex: 0 0 auto;
  color: var(--el-text-color-secondary);
  line-height: 20px;
  white-space: nowrap;
}

.art-table .pagination .el-pagination {
  flex: 1 1 auto;
  justify-content: flex-end;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --dir hunyuan-design exec vitest run packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts --dom`

Expected: PASS with the new count-only summary rendered as `共 128 条`.

- [ ] **Step 5: Commit**

```bash
git add \
  hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/index.ts \
  hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/style.css \
  hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts
git commit -m "feat: add art table pagination summary modes"
```

## Task 2: Recreate the Employee Page Shell and Left Tree Panel

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-org-tree.vue`
- Test: visual inspection of `员工管理` page after local run plus `@hunyuan/system` typecheck

**Interfaces:**
- Consumes:
  - `selectedDepartmentId: Ref<null | number>`
  - `EmployeeOrgTree` emits `select(departmentId: null | number)`
- Produces:
  - same public page/component APIs
  - updated shell classes for the high-fidelity two-column work surface

- [ ] **Step 1: Write the failing test**

```ts
// Visual contract to verify manually after implementation:
// 1. Left column stays at 288px on desktop.
// 2. Tree card header, body, and current node styling match the reference image.
// 3. Browser-level scrollbar does not appear.
```

- [ ] **Step 2: Run test to verify it fails**

Run: open the current `员工管理` page and compare against the reference image.

Expected: FAIL on at least these points:
- tree selection styling is too generic
- tree card spacing does not match the reference
- page-level working-surface rhythm still reads like shared components assembled together

- [ ] **Step 3: Write minimal implementation**

```vue
<Page auto-content-height content-class="employee-page-shell !p-3 h-full min-h-0 overflow-hidden">
  <div class="employee-page">
    <EmployeeOrgTree ... />
    <EmployeeTablePanel ... />
  </div>
</Page>
```

```css
.employee-page-shell {
  background: var(--background-deep);
}

.employee-page {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
```

```css
.employee-org-tree {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  min-height: 0;
  overflow: hidden;
}

.employee-org-tree :deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 16px 24px;
}

.employee-org-tree :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: calc(100% - 55px);
  padding: 14px 14px 12px;
}

.employee-org-tree :deep(.el-tree-node__content) {
  height: 42px;
  border-radius: 10px;
  padding-inline: 10px 8px;
}

.employee-org-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: color-mix(in srgb, var(--el-color-primary) 10%, white);
  color: var(--el-color-primary);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`

Expected: PASS

Run: reload the `员工管理` page and compare the shell and tree area to the reference image.

Expected: PASS on shell rhythm, left tree width, tree header density, and selected tree node appearance.

- [ ] **Step 5: Commit**

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue \
  hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-org-tree.vue
git commit -m "feat: recreate employee page shell and org tree"
```

## Task 3: Recreate the Search Bar, Toolbar, Table Presentation, and Footer Pagination

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue`
- Consumes: Task 1 `summaryMode: 'count'`
- Produces:
  - a page-local high-fidelity search card
  - a page-local high-fidelity toolbar and table presentation
  - employee page pagination using `summaryMode: 'count'`

**Interfaces:**
- Consumes:
  - `ArtSearchPanel`
  - `ArtTableHeader`
  - `ArtTable`
  - `summaryMode: 'count'`
- Produces:
  - same emits: `add`, `department-change`, `edit`, `total-change`
  - updated `paginationOptions` for employee-page recreation

- [ ] **Step 1: Write the failing test**

```ts
// Visual contract to verify manually after implementation:
// 1. Search row is a single horizontal bar with aligned labels and controls.
// 2. '新增员工' remains the primary anchor and right-side tool buttons stay visually secondary.
// 3. Table rows show avatar, name hierarchy, soft role tags, and lighter borders.
// 4. Pagination shows total count, page-size select, pages, and jumper in one footer region.
```

- [ ] **Step 2: Run test to verify it fails**

Run: open the current `员工管理` page and compare against the reference image.

Expected: FAIL on at least these points:
- search row still feels like generic shared-form composition
- toolbar buttons and table card still feel too component-default
- role tags and row presentation do not match the reference
- pagination lacks the full footer composition

- [ ] **Step 3: Write minimal implementation**

```ts
const paginationOptions = {
  align: 'left' as const,
  hideOnSinglePage: true,
  layout: 'sizes, prev, pager, next, jumper',
  pageSizes: [10, 20, 30],
  summaryMode: 'count' as const,
  size: 'small' as const,
}
```

```vue
<ArtTableHeader
  v-model="columnChecks"
  :loading="loading"
  :show-search-bar="showSearchBar"
  layout="search,size,fullscreen,columns,settings"
  class="employee-table-panel__toolbar"
  @search="handleToggleSearchBar"
>
  <template #left>
    <ElSpace class="employee-table-panel__primary-actions">
      <ElButton type="primary" @click="emit('add')">新增员工</ElButton>
      <ElButton :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        批量删除
      </ElButton>
    </ElSpace>
  </template>
</ArtTableHeader>
```

```vue
<template #roleNameList="{ row }">
  <ElTag
    class="employee-table-panel__role-tag"
    :class="`employee-table-panel__role-tag--${getRoleTagType(getPrimaryRole(row))}`"
    effect="light"
    size="small"
  >
    {{ getPrimaryRole(row) }}
  </ElTag>
</template>
```

```css
.employee-table-panel__search-card :deep(.el-card__body) {
  padding: 22px 24px;
}

.employee-table-panel :deep(.art-search-panel__form) {
  align-items: center;
}

.employee-table-panel :deep(.art-search-panel .el-form-item__label) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.employee-table-panel :deep(.art-table-header) {
  margin-bottom: 18px;
}

.employee-table-panel :deep(.art-table .el-table) {
  border-color: color-mix(in srgb, var(--el-border-color-lighter) 82%, white);
}

.employee-table-panel :deep(.art-table .el-table th.el-table__cell) {
  height: 52px;
  color: #6b7280;
  background: #fff;
}

.employee-table-panel :deep(.art-table .el-table td.el-table__cell) {
  padding: 16px 0;
}

.employee-table-panel__avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(180deg, #f3f4f6 0%, #e5e7eb 100%);
  color: var(--el-text-color-secondary);
}

.employee-table-panel__role-tag {
  border: 0;
  border-radius: 999px;
  padding-inline: 10px;
  font-weight: 600;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`

Expected: PASS

Run: visually compare the recreated employee page against the reference image.

Expected: PASS on search-row rhythm, primary/secondary toolbar emphasis, table density, role tags, and footer pagination composition.

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue
git commit -m "feat: recreate employee table panel presentation"
```

## Task 4: Align the Employee Dialog Form with the New Page Family Standard and Run Final Verification

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-form.vue`
- Test:
  - `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`
  - `pnpm --dir hunyuan-design exec vitest run packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts --dom`

**Interfaces:**
- Consumes:
  - existing dialog props and emits
- Produces:
  - same dialog behavior with refined visual hierarchy

- [ ] **Step 1: Write the failing test**

```ts
// Visual contract to verify manually after implementation:
// 1. Employee dialog reads as part of the same page family, not a default Element Plus popup.
// 2. Success state keeps the same calm visual language as the recreated list page.
// 3. No business behavior changes in add/edit flows.
```

- [ ] **Step 2: Run test to verify it fails**

Run: open the add-employee dialog from the current page.

Expected: FAIL because the dialog still reads like a generic component-default popup and the success state is not yet aligned with the recreated page language.

- [ ] **Step 3: Write minimal implementation**

```vue
<ElDialog
  :model-value="visible"
  :title="title"
  width="600px"
  class="employee-form-dialog"
  @close="handleClose"
>
```

```css
.employee-form-dialog :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

.employee-form-dialog :deep(.el-dialog__header) {
  padding: 18px 24px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.employee-form-dialog :deep(.el-dialog__body) {
  padding: 20px 24px 12px;
}

.employee-form-dialog :deep(.el-dialog__footer) {
  padding: 12px 24px 20px;
}

.password-result__content {
  background: #f8fafc;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --dir hunyuan-design exec vitest run packages/@vben/art-hooks/src/table/components/art-table/__tests__/art-table-pagination.test.ts --dom`

Expected: PASS

Run: `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`

Expected: PASS

Run: manually open `新增员工` and `编辑员工`, then compare the visual result against the page-family standard.

Expected: PASS on dialog density, footer actions, and success-state calmness.

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-form.vue
git commit -m "feat: align employee dialog with recreated page"
```

## Self-Review

### Spec coverage

- Page shell, tree panel, search bar, toolbar, table body, pagination, and dialog are all covered by Tasks 1-4.
- Shared extraction is intentionally narrow and limited to pagination summary behavior plus footer layout hooks.
- No task changes business APIs or workflow semantics.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” steps remain.
- Each task lists exact files, commands, and code snippets.

### Type consistency

- Shared pagination API addition uses one exact property name: `summaryMode`.
- Employee table panel consumes `summaryMode: 'count'`.
- Existing employee page emits and props remain unchanged.
