# System Settings Parameter Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the system-settings `module-bridge` entry for parameter configuration with a real backend-wired Vue page at `views/support/config/config-list.vue`.

**Architecture:** Keep the existing real-login, backend-menu loading, and dynamic-route mapping unchanged. Add one focused `config.ts` API module, one source-level support-module contract test, one API payload test file, and one list/dialog page that follows the existing `department` and `position` management page pattern.

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc

## Global Constraints

- Follow `AGENTS.md`: make one incremental change at a time.
- Follow `AGENTS.md`: explain why a change is needed before editing files.
- Follow `AGENTS.md`: prefer existing project patterns over new abstractions.
- Follow `AGENTS.md`: do not add new dependencies without explicit approval.
- Follow `docs/frontend-list-table-page-standard.md` for list/search/table pages.
- Strictly use the backend menu component path `/support/config/config-list.vue`; do not invent a different route or page path.
- Keep login, backend menu loading, `login-adapter.ts`, and `module-bridge` behavior unchanged.
- Keep request logic in `apps/hunyuan-system/src/api/system/*.ts`, not in shared `@vben/art-hooks` components.
- Implement only the parameter-config increment in this plan; dictionary, file, job, message, serial number, cache, and reload each need separate follow-up plans.
- Use UTF-8 for reads and writes.
- Use concise Chinese code comments only where logic is not self-explanatory.
- Verify meaningful frontend changes with `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`.

---

## File Structure

### Test Contracts

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
  - Lock the existence, page-shape, and endpoint wiring of the parameter-config module.

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts`
  - Lock query payload trimming and mutation payload normalization.

### API Module

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/config.ts`
  - Define `PageResult`, `ConfigRecord`, query DTOs, form DTOs, payload builders, and request functions.
  - Keep parameter-config API independent from `organization.ts` and `menu.ts`.

### Page Module

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/config/config-list.vue`
  - Render a dense search + table + dialog page using `Page`, `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, and `ArtTable`.
  - Provide query, add, and edit flows.
  - Do not add extra hero/title/description chrome.

## Task 1: Lock the Parameter Config Contract with Source and API Tests

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts`

**Interfaces:**
- Consumes:
  - `existsSync`, `readFileSync`, `resolve` from Node fs/path
  - `describe`, `expect`, `it` from Vitest
- Produces:
  - Source-level checks that the parameter-config page exists at the backend-defined path.
  - API-level checks that query and mutation payloads trim text correctly.

- [ ] **Step 1: Create the failing source contract**

Create `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts` with:

```ts
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const configPagePath = 'apps/hunyuan-system/src/views/support/config/config-list.vue';
const configApiPath = 'apps/hunyuan-system/src/api/system/config.ts';

describe('system settings support modules', () => {
  it('provides a real parameter config page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), configPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportConfigList');
  });

  it('keeps the parameter config page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), configPagePath), 'utf8');

    expect(source).not.toContain('config-page__title');
    expect(source).not.toContain('config-page__hero');
    expect(source).not.toContain('config-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires the parameter config api module to the backend config endpoints', () => {
    const apiPath = resolve(process.cwd(), configApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/config/query'");
    expect(source).toContain("'/config/add'");
    expect(source).toContain("'/config/update'");
    expect(source).toContain('buildConfigPageQueryPayload');
    expect(source).toContain('buildConfigMutationPayload');
  });

  it('surfaces the config key, name, value, and remark fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), configPagePath), 'utf8');

    expect(source).toContain('configKey');
    expect(source).toContain('configName');
    expect(source).toContain('configValue');
    expect(source).toContain('remark');
  });
});
```

- [ ] **Step 2: Create the failing API payload test**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts` with:

```ts
import { describe, expect, it } from 'vitest';

import {
  buildConfigMutationPayload,
  buildConfigPageQueryPayload,
} from './config';

describe('parameter config api payloads', () => {
  it('trims config query keywords and preserves paging fields', () => {
    expect(
      buildConfigPageQueryPayload({
        configKey: '  system.demo.key  ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      configKey: 'system.demo.key',
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('omits blank config query keywords after trimming', () => {
    expect(
      buildConfigPageQueryPayload({
        configKey: '   ',
        pageNum: 1,
        pageSize: 10,
      }),
    ).toEqual({
      configKey: undefined,
      pageNum: 1,
      pageSize: 10,
    });
  });

  it('trims config mutation payload fields and preserves configId on update', () => {
    expect(
      buildConfigMutationPayload({
        configId: 9,
        configKey: '  system.welcome.text  ',
        configName: '  欢迎文案  ',
        configValue: '  欢迎使用混元系统  ',
        remark: '  首页配置  ',
      }),
    ).toEqual({
      configId: 9,
      configKey: 'system.welcome.text',
      configName: '欢迎文案',
      configValue: '欢迎使用混元系统',
      remark: '首页配置',
    });
  });
});
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/config.test.ts --dom
```

Expected:

- The source contract fails because `config-list.vue` and `config.ts` do not exist yet.
- The API payload test fails because `./config` does not exist yet.

- [ ] **Step 4: Do not commit the failing tests alone unless work must pause**

If a pause is required, use:

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts \
  hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts
git commit -m "test: add parameter config frontend contracts"
```

## Task 2: Add the Parameter Config API Module

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/config.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts`

**Interfaces:**
- Consumes: `requestClient` from `#/api/request`
- Produces:
  - `PageResult<T>`
  - `ConfigRecord`
  - `ConfigPageQueryParams`
  - `ConfigAddForm`
  - `ConfigUpdateForm`
  - `buildConfigPageQueryPayload(params: ConfigPageQueryParams)`
  - `buildConfigMutationPayload<T extends ConfigAddForm | ConfigUpdateForm>(params: T)`
  - `queryConfigPage(params: ConfigPageQueryParams)`
  - `addConfig(params: ConfigAddForm)`
  - `updateConfig(params: ConfigUpdateForm)`

- [ ] **Step 1: Create the API file**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/config.ts` with:

```ts
import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface ConfigRecord {
  configId: number;
  configKey: string;
  configName: string;
  configValue: string;
  remark?: null | string;
  createTime?: null | string;
  updateTime?: null | string;
}

export interface ConfigPageQueryParams {
  configKey?: string;
  pageNum: number;
  pageSize: number;
}

export interface ConfigAddForm {
  configKey: string;
  configName: string;
  configValue: string;
  remark?: null | string;
}

export interface ConfigUpdateForm extends ConfigAddForm {
  configId: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildConfigPageQueryPayload(params: ConfigPageQueryParams) {
  const configKey = cleanText(params.configKey);

  return {
    configKey: configKey || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export function buildConfigMutationPayload<
  T extends ConfigAddForm | ConfigUpdateForm,
>(params: T): T {
  return {
    ...params,
    configKey: params.configKey.trim(),
    configName: params.configName.trim(),
    configValue: params.configValue.trim(),
    remark: cleanText(params.remark),
  };
}

export async function queryConfigPage(params: ConfigPageQueryParams) {
  return requestClient.post<PageResult<ConfigRecord>>(
    '/config/query',
    buildConfigPageQueryPayload(params),
  );
}

export async function addConfig(params: ConfigAddForm) {
  return requestClient.post<string>(
    '/config/add',
    buildConfigMutationPayload(params),
  );
}

export async function updateConfig(params: ConfigUpdateForm) {
  return requestClient.post<string>(
    '/config/update',
    buildConfigMutationPayload(params),
  );
}
```

- [ ] **Step 2: Run the API payload test**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/config.test.ts --dom
```

Expected: PASS.

- [ ] **Step 3: Run typecheck to verify the new API module compiles**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/api/system/config.ts \
  hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts
git commit -m "feat: add parameter config api module"
```

## Task 3: Add the Real Parameter Config Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/config/config-list.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

**Interfaces:**
- Consumes:
  - `ConfigAddForm`, `ConfigPageQueryParams`, `ConfigRecord`, `ConfigUpdateForm`
  - `addConfig`, `queryConfigPage`, `updateConfig`
- Produces:
  - Vue component name `SystemSupportConfigList`
  - Real page file at backend-defined component path `/support/config/config-list.vue`
  - Search + table + dialog workflow for parameter-config records

- [ ] **Step 1: Create the Vue page**

Create `hunyuan-design/apps/hunyuan-system/src/views/support/config/config-list.vue` with:

```vue
<script setup lang="ts">
import type {
  ConfigAddForm,
  ConfigRecord,
  ConfigUpdateForm,
} from '#/api/system/config';
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
  ElMessage,
  ElSpace,
} from 'element-plus';

import {
  addConfig,
  queryConfigPage,
  updateConfig,
} from '#/api/system/config';

defineOptions({ name: 'SystemSupportConfigList' });

interface ConfigFormModel extends ConfigAddForm {
  configId?: number;
}

const loading = ref(false);
const keyword = ref('');
const showSearchBar = ref(true);
const rows = ref<ConfigRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formData = reactive<ConfigFormModel>({
  configKey: '',
  configName: '',
  configValue: '',
  remark: '',
});

const rules: FormRules<ConfigFormModel> = {
  configKey: [{ required: true, message: '请输入参数 Key', trigger: 'blur' }],
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<ConfigRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'configKey', label: '参数 Key', minWidth: 220 },
  { prop: 'configName', label: '参数名称', minWidth: 180 },
  {
    prop: 'configValue',
    label: '参数值',
    minWidth: 260,
    formatter: (row) => row.configValue || '-',
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 200,
    formatter: (row) => row.remark || '-',
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 96,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resetForm() {
  Object.assign(formData, {
    configKey: '',
    configName: '',
    configValue: '',
    remark: '',
  });
  formData.configId = undefined;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryConfigPage({
      configKey: keyword.value,
      pageNum: pagination.current,
      pageSize: pagination.size,
    });
    rows.value = result?.list ?? [];
    pagination.total = result?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  keyword.value = '';
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openAddDialog() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(row: ConfigRecord) {
  dialogMode.value = 'edit';
  Object.assign(formData, {
    configId: row.configId,
    configKey: row.configKey,
    configName: row.configName,
    configValue: row.configValue,
    remark: row.remark || '',
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addConfig(formData as ConfigAddForm);
    ElMessage.success('新增参数配置成功');
  } else {
    await updateConfig(formData as ConfigUpdateForm);
    ElMessage.success('更新参数配置成功');
  }

  dialogVisible.value = false;
  await loadData();
}

function handleCurrentChange(value: number) {
  pagination.current = value;
  void loadData();
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
  void loadData();
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '参数配置数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="config-page">
      <ElCard
        v-show="showSearchBar"
        class="config-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="config-page__keyword-item" label="参数 Key">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入参数 Key"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="config-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">
                新增参数
              </ElButton>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="columns"
            :data="rows"
            :height="tableHeight"
            :loading="loading"
            :pagination="pagination"
            :pagination-options="{
              align: 'center',
              hideOnSinglePage: false,
              layout: 'sizes, prev, pager, next, jumper',
              pageSizes: [10, 20, 30],
              showTotalSummary: true,
              size: 'small',
            }"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #actions="{ row }">
              <ElSpace class="config-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openEditDialog(row)"
                >
                  编辑
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增参数配置' : '编辑参数配置'"
      width="640px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <ElFormItem label="参数 Key" prop="configKey">
          <ElInput v-model="formData.configKey" placeholder="请输入参数 Key" />
        </ElFormItem>
        <ElFormItem label="参数名称" prop="configName">
          <ElInput v-model="formData.configName" placeholder="请输入参数名称" />
        </ElFormItem>
        <ElFormItem label="参数值" prop="configValue">
          <ElInput
            v-model="formData.configValue"
            :rows="4"
            placeholder="请输入参数值"
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="备注" prop="remark">
          <ElInput
            v-model="formData.remark"
            maxlength="255"
            placeholder="请输入备注"
            type="textarea"
          />
        </ElFormItem>
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

<style scoped>
.config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.config-page__search-card,
.config-page__table-card {
  border-radius: 8px;
}

.config-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.config-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.config-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.config-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.config-page :deep(.art-table-panel),
.config-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.config-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.config-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.config-page__keyword-item :deep(.el-form-item__content) {
  width: 260px;
}

.config-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.config-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.config-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .config-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
```

- [ ] **Step 2: Run the support-module source contract**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
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
  hunyuan-design/apps/hunyuan-system/src/views/support/config/config-list.vue \
  hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts
git commit -m "feat: add parameter config management page"
```

## Task 4: Verify Dynamic Route Matching and Increment Closure

**Files:**
- Modify only if targeted verification reveals a direct issue:
  - `hunyuan-design/apps/hunyuan-system/src/api/system/config.ts`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/config/config-list.vue`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
  - `hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts`

**Interfaces:**
- Consumes:
  - Existing backend-menu loading via `apps/hunyuan-system/src/api/core/auth.ts`
  - Existing dynamic-route adapter via `apps/hunyuan-system/src/api/core/login-adapter.ts`
  - Backend menu component path `/support/config/config-list.vue`
- Produces:
  - Verified first system-settings increment with no route-framework changes

- [ ] **Step 1: Run all targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/config.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all PASS.

- [ ] **Step 2: Verify source-level route matching assumptions**

Run:

```bash
rg -n "normalizeComponentPath|module-bridge|/support/config/config-list.vue|SystemSupportConfigList" hunyuan-design/apps/hunyuan-system/src
```

Expected output includes:

- `apps/hunyuan-system/src/api/core/login-adapter.ts` normalization logic
- `apps/hunyuan-system/src/views/system/module-bridge/index.vue`
- `apps/hunyuan-system/src/views/support/config/config-list.vue`
- `defineOptions({ name: 'SystemSupportConfigList' })`

- [ ] **Step 3: Check final working tree scope**

Run:

```bash
git status --short
```

Expected:

- Intended tracked changes only for the parameter-config increment
- Pre-existing unrelated untracked files may still remain, such as:

```text
.playwright-mcp/
.superpowers/
hunyuan-system-home-snapshot.md
lefthook.yml
```

- [ ] **Step 4: Commit any final correction if verification required a fix**

If a correction was required, commit it:

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/api/system/config.ts \
  hunyuan-design/apps/hunyuan-system/src/api/system/config.test.ts \
  hunyuan-design/apps/hunyuan-system/src/views/support/config/config-list.vue \
  hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts
git commit -m "fix: tighten parameter config route closure"
```

If no correction was required, do not create an empty commit.

## Self-Review

### Spec coverage

- Backend-defined component path `/support/config/config-list.vue` is implemented directly in Task 3.
- Query/add/update API closure is implemented in Task 2 and consumed in Task 3.
- Dense list/search/table page standard is implemented in Task 3 and checked in Task 1.
- Dynamic route closure is verified in Task 4 without changing login/menu framework code.
- Dictionary, file, job, message, serial number, cache, and reload are intentionally excluded from this plan.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every code step contains explicit code.
- Every verification step contains exact commands and expected outcomes.

### Type consistency

- `ConfigRecord`, `ConfigPageQueryParams`, `ConfigAddForm`, and `ConfigUpdateForm` are defined in Task 2 and consumed consistently in Task 3.
- `buildConfigPageQueryPayload` and `buildConfigMutationPayload` are defined in Task 2 and asserted in both Task 1 and Task 2.
- The page component name is consistently `SystemSupportConfigList`.
