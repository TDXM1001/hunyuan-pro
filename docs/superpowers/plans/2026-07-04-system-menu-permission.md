# System Menu Permission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first system-management menu page in `hunyuan-system`, closing the menu tree, route path, component path, button permission code, and role-permission visibility loop.

**Architecture:** Keep the existing real-login and backend-menu loading chain unchanged. Add a focused `menu.ts` API module, a single menu-management Vue page that follows the existing department/position page structure, and source-level tests that lock the page standard and backend endpoint wiring.

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc

## Global Constraints

- Follow `AGENTS.md`: make one incremental change at a time.
- Follow `AGENTS.md`: explain why a change is needed before editing files.
- Follow `AGENTS.md`: prefer existing project patterns over new abstractions.
- Follow `AGENTS.md`: do not add new dependencies without explicit approval.
- Follow `docs/frontend-list-table-page-standard.md` for list/search/table pages.
- Do not implement dictionary management, config management, login logs, or operate logs in this first increment.
- Do not change login, backend menu loading, role page structure, or shared route generation.
- Do not bind backend APIs into shared `@vben/art-hooks` components.
- Keep `module-bridge` for pages that still do not have local Vue implementations.
- Verify meaningful frontend changes with `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`.

---

## File Structure

### Test Contract

- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`
  - Add source-level tests for the menu page, menu API module, list-page standard, and permission fields.

### API Module

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts`
  - Define menu DTOs, mutation forms, payload builders, and request functions.
  - Keep menu API separate from `organization.ts` so the organization API file does not continue to grow.

### Menu Page

- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue`
  - Render a tree table for menu records.
  - Use `Page`, `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, `ArtTable`, and `ElDialog`.
  - Provide add-root, add-child, edit, delete, search, and refresh flows.
  - Keep ordinary menu-backed page chrome quiet: no page hero, duplicate title, or explanatory copy.

## Task 1: Lock the Menu Management Contract with Source Tests

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes: existing source-test pattern using `existsSync`, `readFileSync`, `resolve`, `describe`, `expect`, `it`.
- Produces: tests that later tasks must satisfy:
  - `apps/hunyuan-system/src/api/system/menu.ts` exists.
  - `apps/hunyuan-system/src/views/system/menu/index.vue` exists.
  - menu page uses shared table/search primitives.
  - menu page exposes `path`, `component`, `webPerms`, and `apiPerms`.
  - menu page stays quiet without hero/title/description blocks.

- [ ] **Step 1: Add failing test cases**

Append the following constants near the existing path constants:

```ts
const menuPagePath = 'apps/hunyuan-system/src/views/system/menu/index.vue';
const menuApiPath = 'apps/hunyuan-system/src/api/system/menu.ts';
```

Append these tests inside `describe('organization backend menu docking pages', () => { ... })`:

```ts
  it('provides a real menu management page', () => {
    const pagePath = resolve(process.cwd(), menuPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('row-key="menuId"');
    expect(source).toContain(':tree-props="{ children: \'children\' }"');
  });

  it('keeps menu management dense without extra page title or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).not.toContain('menu-page__title');
    expect(source).not.toContain('menu-page__hero');
    expect(source).not.toContain('menu-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires menu management to backend menu endpoints', () => {
    const apiPath = resolve(process.cwd(), menuApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/menu/query'");
    expect(source).toContain("'/menu/tree'");
    expect(source).toContain("'/menu/add'");
    expect(source).toContain("'/menu/update'");
    expect(source).toContain("'/menu/batchDelete'");
    expect(source).toContain("'/menu/auth/url'");
  });

  it('surfaces route, component, and permission fields on the menu page', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).toContain('path');
    expect(source).toContain('component');
    expect(source).toContain('webPerms');
    expect(source).toContain('apiPerms');
    expect(source).toContain('frameFlag');
    expect(source).toContain('visibleFlag');
    expect(source).toContain('disabledFlag');
  });

  it('keeps menu row actions compact and measurable', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).toContain('class="menu-page__actions"');
    expect(source).toContain('.menu-page__actions {');
    expect(source).toContain('display: inline-flex;');
    expect(source).toContain('gap: 8px;');
    expect(source).toContain('.menu-page__actions :deep(.el-button)');
    expect(source).toContain('font-size: 14px;');
    expect(source).toContain('line-height: 22px;');
    expect(source).toContain('padding: 0;');
    expect(source).toContain('.menu-page__actions :deep(.el-button + .el-button)');
    expect(source).toContain('margin-left: 0;');
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
```

Expected: FAIL because `apps/hunyuan-system/src/api/system/menu.ts` and `apps/hunyuan-system/src/views/system/menu/index.vue` do not exist yet.

- [ ] **Step 3: Commit the failing contract only after confirming failure is expected**

Do not commit this task alone unless the implementation will be paused. If committing is needed, use:

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts
git commit -m "test: add menu management frontend contract"
```

## Task 2: Add the Menu API Module

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes: `requestClient` from `#/api/request`.
- Produces:
  - `MenuRecord`
  - `MenuTreeRecord`
  - `MenuAddForm`
  - `MenuUpdateForm`
  - `RequestUrlRecord`
  - `queryMenus(): Promise<MenuRecord[]>`
  - `queryMenuTree(onlyMenu: boolean): Promise<MenuTreeRecord[]>`
  - `getMenuDetail(menuId: number): Promise<MenuRecord>`
  - `addMenu(params: MenuAddForm): Promise<string>`
  - `updateMenu(params: MenuUpdateForm): Promise<string>`
  - `batchDeleteMenus(menuIdList: number[]): Promise<string>`
  - `listAuthUrls(): Promise<RequestUrlRecord[]>`
  - `buildMenuMutationPayload<T extends MenuAddForm | MenuUpdateForm>(params: T): T`

- [ ] **Step 1: Create the API file**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts` with this content:

```ts
import { requestClient } from '#/api/request';

export interface MenuRecord {
  apiPerms?: null | string;
  cacheFlag: boolean;
  component?: null | string;
  contextMenuId?: null | number;
  createTime?: null | string;
  disabledFlag: boolean;
  frameFlag: boolean;
  frameUrl?: null | string;
  icon?: null | string;
  menuId: number;
  menuName: string;
  menuType: number;
  parentId: number;
  path?: null | string;
  permsType?: null | number;
  sort?: null | number;
  updateTime?: null | string;
  visibleFlag: boolean;
  webPerms?: null | string;
}

export interface MenuTreeRecord extends MenuRecord {
  children?: MenuTreeRecord[];
}

export interface RequestUrlRecord {
  method?: null | string;
  name?: null | string;
  url?: null | string;
}

export interface MenuAddForm {
  apiPerms?: null | string;
  cacheFlag: boolean;
  component?: null | string;
  contextMenuId?: null | number;
  disabledFlag: boolean;
  frameFlag: boolean;
  frameUrl?: null | string;
  icon?: null | string;
  menuName: string;
  menuType: number;
  parentId: number;
  path?: null | string;
  permsType?: null | number;
  sort?: null | number;
  visibleFlag: boolean;
  webPerms?: null | string;
}

export interface MenuUpdateForm extends MenuAddForm {
  menuId: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildMenuMutationPayload<
  T extends MenuAddForm | MenuUpdateForm,
>(params: T): T {
  return {
    ...params,
    apiPerms: cleanText(params.apiPerms),
    component: cleanText(params.component),
    contextMenuId: params.contextMenuId ?? null,
    frameUrl: cleanText(params.frameUrl),
    icon: cleanText(params.icon),
    menuName: params.menuName.trim(),
    parentId: params.parentId ?? 0,
    path: cleanText(params.path),
    permsType: params.permsType ?? null,
    sort: params.sort ?? 0,
    webPerms: cleanText(params.webPerms),
  };
}

export async function queryMenus() {
  return requestClient.get<MenuRecord[]>('/menu/query');
}

export async function queryMenuTree(onlyMenu: boolean) {
  return requestClient.get<MenuTreeRecord[]>('/menu/tree', {
    params: { onlyMenu },
  });
}

export async function getMenuDetail(menuId: number) {
  return requestClient.get<MenuRecord>(`/menu/detail/${menuId}`);
}

export async function addMenu(params: MenuAddForm) {
  return requestClient.post<string>(
    '/menu/add',
    buildMenuMutationPayload(params),
  );
}

export async function updateMenu(params: MenuUpdateForm) {
  return requestClient.post<string>(
    '/menu/update',
    buildMenuMutationPayload(params),
  );
}

export async function batchDeleteMenus(menuIdList: number[]) {
  return requestClient.get<string>('/menu/batchDelete', {
    params: { menuIdList },
  });
}

export async function listAuthUrls() {
  return requestClient.get<RequestUrlRecord[]>('/menu/auth/url');
}
```

- [ ] **Step 2: Run the source contract**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
```

Expected: still FAIL because the Vue page does not exist, while the API endpoint assertions now pass.

- [ ] **Step 3: Run typecheck**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts
git commit -m "feat: add system menu api module"
```

## Task 3: Add the Menu Management Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes:
  - `MenuAddForm`, `MenuRecord`, `MenuTreeRecord`, `MenuUpdateForm` from `#/api/system/menu`
  - `addMenu`, `batchDeleteMenus`, `queryMenuTree`, `updateMenu`
- Produces:
  - Vue component name `SystemMenuManagement`
  - Tree-table page with `row-key="menuId"`
  - Dialog form with explicit fields for route/component/permission codes

- [ ] **Step 1: Create the Vue page**

Create `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue` with this structure:

```vue
<script setup lang="ts">
import type {
  MenuAddForm,
  MenuRecord,
  MenuTreeRecord,
  MenuUpdateForm,
} from '#/api/system/menu';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addMenu,
  batchDeleteMenus,
  queryMenuTree,
  updateMenu,
} from '#/api/system/menu';

defineOptions({ name: 'SystemMenuManagement' });

interface MenuTreeRow extends MenuRecord {
  children?: MenuTreeRow[];
  level: number;
}

interface MenuFormModel extends MenuAddForm {
  menuId?: number;
}

const MENU_TYPE_OPTIONS = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '功能点', value: 3 },
] as const;

const PERMS_TYPE_OPTIONS = [
  { label: '无需权限', value: 0 },
  { label: '前端权限', value: 1 },
  { label: '后端权限', value: 2 },
  { label: '前后端权限', value: 3 },
] as const;

const loading = ref(false);
const keyword = ref('');
const showSearchBar = ref(true);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const addParentName = ref('');
const formRef = ref<FormInstance>();
const rawTree = ref<MenuTreeRecord[]>([]);

const formData = reactive<MenuFormModel>({
  apiPerms: '',
  cacheFlag: false,
  component: '',
  contextMenuId: null,
  disabledFlag: false,
  frameFlag: false,
  frameUrl: '',
  icon: '',
  menuName: '',
  menuType: 2,
  parentId: 0,
  path: '',
  permsType: 0,
  sort: 100,
  visibleFlag: true,
  webPerms: '',
});

const rules: FormRules<MenuFormModel> = {
  cacheFlag: [{ required: true, message: '请选择是否缓存', trigger: 'change' }],
  disabledFlag: [{ required: true, message: '请选择禁用状态', trigger: 'change' }],
  frameFlag: [{ required: true, message: '请选择是否外链', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  visibleFlag: [{ required: true, message: '请选择显示状态', trigger: 'change' }],
};

const columnsFactory = (): ColumnOption<MenuTreeRow>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'menuName', label: '菜单名称', minWidth: 240, useSlot: true },
  { prop: 'path', label: '路由路径', minWidth: 180, formatter: (row) => row.path || '-' },
  {
    prop: 'component',
    label: '组件路径',
    minWidth: 220,
    formatter: (row) => row.component || '-',
  },
  {
    prop: 'webPerms',
    label: '前端权限码',
    minWidth: 180,
    formatter: (row) => row.webPerms || '-',
  },
  {
    prop: 'apiPerms',
    label: '后端权限码',
    minWidth: 180,
    formatter: (row) => row.apiPerms || '-',
  },
  { prop: 'sort', label: '排序', width: 90, align: 'center' },
  { prop: 'status', label: '状态', width: 150, align: 'center', useSlot: true },
  {
    prop: 'actions',
    label: '操作',
    width: 180,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

function buildMenuRows(nodes: MenuTreeRecord[], level = 0): MenuTreeRow[] {
  return nodes.map((node) => {
    const children = buildMenuRows(node.children ?? [], level + 1);
    return {
      ...node,
      children: children.length > 0 ? children : undefined,
      level,
      parentId: node.parentId ?? 0,
      sort: node.sort ?? 0,
    };
  });
}

function flattenMenuRows(nodes: MenuTreeRow[]): MenuTreeRow[] {
  return nodes.flatMap((node) => {
    const { children, ...current } = node;
    return [current, ...flattenMenuRows(children ?? [])];
  });
}

const menuTreeRows = computed(() => buildMenuRows(rawTree.value));
const menuRows = computed(() => flattenMenuRows(menuTreeRows.value));
const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return menuTreeRows.value;
  }

  return menuRows.value.filter((item) =>
    [item.menuName, item.path, item.component, item.webPerms, item.apiPerms]
      .filter(Boolean)
      .some((field) => field!.toLowerCase().includes(value)),
  );
});
const parentOptions = computed(() => menuRows.value.filter((item) => item.menuType !== 3));
const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    return '编辑菜单';
  }
  return addParentName.value ? `新增${addParentName.value}下级菜单` : '新增顶级菜单';
});

function getMenuTypeLabel(menuType: number) {
  return MENU_TYPE_OPTIONS.find((item) => item.value === menuType)?.label ?? '未知';
}

function resetForm() {
  Object.assign(formData, {
    apiPerms: '',
    cacheFlag: false,
    component: '',
    contextMenuId: null,
    disabledFlag: false,
    frameFlag: false,
    frameUrl: '',
    icon: '',
    menuName: '',
    menuType: 2,
    parentId: 0,
    path: '',
    permsType: 0,
    sort: 100,
    visibleFlag: true,
  });
  formData.menuId = undefined;
  addParentName.value = '';
}

async function loadData() {
  loading.value = true;
  try {
    rawTree.value = await queryMenuTree(false);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  keyword.value = keyword.value.trim();
}

function handleReset() {
  keyword.value = '';
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openAddRootDialog() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openAddChildDialog(row: MenuTreeRow) {
  dialogMode.value = 'add';
  resetForm();
  formData.parentId = row.menuId;
  formData.contextMenuId = row.menuType === 2 ? row.menuId : null;
  addParentName.value = row.menuName;
  dialogVisible.value = true;
}

function openEditDialog(row: MenuTreeRow) {
  dialogMode.value = 'edit';
  addParentName.value = '';
  Object.assign(formData, {
    apiPerms: row.apiPerms || '',
    cacheFlag: row.cacheFlag,
    component: row.component || '',
    contextMenuId: row.contextMenuId ?? null,
    disabledFlag: row.disabledFlag,
    frameFlag: row.frameFlag,
    frameUrl: row.frameUrl || '',
    icon: row.icon || '',
    menuId: row.menuId,
    menuName: row.menuName,
    menuType: row.menuType,
    parentId: row.parentId ?? 0,
    path: row.path || '',
    permsType: row.permsType ?? 0,
    sort: row.sort ?? 0,
    visibleFlag: row.visibleFlag,
    webPerms: row.webPerms || '',
  });
  dialogVisible.value = true;
}

async function handleDelete(row: MenuTreeRow) {
  try {
    await ElMessageBox.confirm(`确定删除菜单“${row.menuName}”吗？`, '删除确认', {
      type: 'warning',
    });
    await batchDeleteMenus([row.menuId]);
    ElMessage.success('删除成功');
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addMenu(formData as MenuAddForm);
    ElMessage.success('新增菜单成功');
  } else {
    await updateMenu(formData as MenuUpdateForm);
    ElMessage.success('更新菜单成功');
  }

  dialogVisible.value = false;
  await loadData();
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '菜单数据加载失败');
  });
});
</script>
```

Use this template section:

```vue
<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="menu-page">
      <ElCard v-show="showSearchBar" class="menu-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="menu-page__keyword-item" label="关键字">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入菜单、路由、组件或权限码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="menu-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddRootDialog">新增顶级菜单</ElButton>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="columns"
            :data="filteredRows"
            :default-expand-all="true"
            height="100%"
            :indent="24"
            :loading="loading"
            row-key="menuId"
            :tree-props="{ children: 'children' }"
          >
            <template #menuName="{ row }">
              <div
                class="menu-page__tree-name"
                :style="{ paddingLeft: keyword.trim() ? `${row.level * 24}px` : '0px' }"
              >
                <span class="menu-page__tree-label">{{ row.menuName }}</span>
                <ElTag effect="plain" size="small">{{ getMenuTypeLabel(row.menuType) }}</ElTag>
              </div>
            </template>

            <template #status="{ row }">
              <ElSpace :size="6">
                <ElTag :type="row.visibleFlag ? 'success' : 'info'" effect="plain" size="small">
                  {{ row.visibleFlag ? '显示' : '隐藏' }}
                </ElTag>
                <ElTag :type="row.disabledFlag ? 'danger' : 'success'" effect="plain" size="small">
                  {{ row.disabledFlag ? '禁用' : '启用' }}
                </ElTag>
              </ElSpace>
            </template>

            <template #actions="{ row }">
              <ElSpace class="menu-page__actions">
                <ElButton link size="small" type="primary" @click="openAddChildDialog(row)">
                  新增下级
                </ElButton>
                <ElButton link size="small" type="primary" @click="openEditDialog(row)">
                  编辑
                </ElButton>
                <ElButton link size="small" type="danger" @click="handleDelete(row)">
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <div class="menu-page__form-grid">
          <ElFormItem label="菜单名称" prop="menuName">
            <ElInput v-model="formData.menuName" placeholder="请输入菜单名称" />
          </ElFormItem>
          <ElFormItem label="菜单类型" prop="menuType">
            <ElSelect v-model="formData.menuType" placeholder="请选择菜单类型">
              <ElOption
                v-for="item in MENU_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="上级菜单" prop="parentId">
            <ElSelect v-model="formData.parentId" clearable filterable placeholder="请选择上级菜单">
              <ElOption :value="0" label="顶级菜单" />
              <ElOption
                v-for="item in parentOptions"
                :key="item.menuId"
                :disabled="dialogMode === 'edit' && item.menuId === formData.menuId"
                :label="item.menuName"
                :value="item.menuId"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="排序" prop="sort">
            <ElInputNumber v-model="formData.sort" :min="0" style="width: 100%" />
          </ElFormItem>
          <ElFormItem label="路由路径" prop="path">
            <ElInput v-model="formData.path" placeholder="例如 /system/menu" />
          </ElFormItem>
          <ElFormItem label="组件路径" prop="component">
            <ElInput v-model="formData.component" placeholder="例如 /system/menu/index" />
          </ElFormItem>
          <ElFormItem label="前端权限码" prop="webPerms">
            <ElInput v-model="formData.webPerms" placeholder="例如 system:menu:add" />
          </ElFormItem>
          <ElFormItem label="后端权限码" prop="apiPerms">
            <ElInput v-model="formData.apiPerms" placeholder="例如 system:menu:add" />
          </ElFormItem>
          <ElFormItem label="权限类型" prop="permsType">
            <ElSelect v-model="formData.permsType" placeholder="请选择权限类型">
              <ElOption
                v-for="item in PERMS_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="图标" prop="icon">
            <ElInput v-model="formData.icon" placeholder="例如 lucide:settings" />
          </ElFormItem>
          <ElFormItem label="外链地址" prop="frameUrl">
            <ElInput v-model="formData.frameUrl" placeholder="请输入外链地址" />
          </ElFormItem>
          <ElFormItem label="关联菜单ID" prop="contextMenuId">
            <ElInputNumber v-model="formData.contextMenuId" :min="0" style="width: 100%" />
          </ElFormItem>
          <ElFormItem label="是否外链" prop="frameFlag">
            <ElSwitch v-model="formData.frameFlag" />
          </ElFormItem>
          <ElFormItem label="是否缓存" prop="cacheFlag">
            <ElSwitch v-model="formData.cacheFlag" />
          </ElFormItem>
          <ElFormItem label="是否显示" prop="visibleFlag">
            <ElSwitch v-model="formData.visibleFlag" />
          </ElFormItem>
          <ElFormItem label="是否禁用" prop="disabledFlag">
            <ElSwitch v-model="formData.disabledFlag" />
          </ElFormItem>
        </div>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">保存</ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>
```

Use this style section:

```vue
<style scoped>
.menu-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.menu-page__search-card,
.menu-page__table-card {
  border-radius: 8px;
}

.menu-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.menu-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.menu-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.menu-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.menu-page :deep(.art-table-panel),
.menu-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.menu-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.menu-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.menu-page__keyword-item :deep(.el-form-item__content) {
  width: 320px;
}

.menu-page__tree-name {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.menu-page__tree-label {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.menu-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.menu-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.menu-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.menu-page__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (width <= 768px) {
  .menu-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .menu-page__form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 2: Run source contract**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 3: Run typecheck**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue \
  hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts
git commit -m "feat: add system menu management page"
```

## Task 4: Verify the Permission Loop and Record the Result

**Files:**
- Modify only if verification reveals a direct issue:
  - `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue`
  - `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts`
  - `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes:
  - existing backend-menu loading in `apps/hunyuan-system/src/router/access.ts`
  - existing role authorization APIs in `apps/hunyuan-system/src/api/system/organization.ts`
  - existing `module-bridge`
- Produces:
  - verified first-stage menu-management increment

- [ ] **Step 1: Run all targeted frontend checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: both PASS.

- [ ] **Step 2: Review the permission loop from source**

Confirm these exact source facts:

```bash
rg -n "getAllMenusApi|fetchMenuListAsync|module-bridge|getRoleSelectedMenu|updateRoleMenu|SystemMenuManagement" hunyuan-design/apps/hunyuan-system/src
```

Expected output includes:

- `apps/hunyuan-system/src/router/access.ts` calling `getAllMenusApi()`.
- `apps/hunyuan-system/src/views/system/module-bridge/index.vue` still present.
- `apps/hunyuan-system/src/views/system/role/index.vue` using `getRoleSelectedMenu` and `updateRoleMenu`.
- `apps/hunyuan-system/src/views/system/menu/index.vue` defining `SystemMenuManagement`.

- [ ] **Step 3: Check final working tree**

Run:

```bash
git status --short
```

Expected: only intended files modified, plus pre-existing unrelated untracked files if they were already present:

- `.playwright-mcp/`
- `hunyuan-system-home-snapshot.md`
- `lefthook.yml`

- [ ] **Step 4: Commit any final correction**

If Task 4 required a correction, commit it:

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts \
  hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue \
  hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts
git commit -m "fix: tighten system menu permission loop"
```

If no correction was needed, do not create an empty commit.

## Self-Review

### Spec coverage

- Menu tree, route path, component path, external link fields, visibility, disabled state, and sort are covered by Task 3.
- Button permission fields `webPerms` and `apiPerms` are covered by Task 2 API types and Task 3 page form/table.
- Role permission loop is covered by Task 4 source verification and preserved role page APIs.
- `module-bridge` remains unchanged.
- Dictionary, config, login log, and operate log pages are excluded from the first increment.

### Placeholder scan

- No red-flag placeholder steps remain.
- Each task lists exact files, commands, and expected results.
- Code steps include concrete code blocks instead of vague instructions.

### Type consistency

- `MenuRecord`, `MenuTreeRecord`, `MenuAddForm`, and `MenuUpdateForm` are defined in Task 2 and consumed by Task 3.
- Function names are consistent across tasks: `queryMenuTree`, `addMenu`, `updateMenu`, `batchDeleteMenus`.
- Page component name is consistently `SystemMenuManagement`.
