# SMS Management Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前分支为 `hunyuan-system` 落地短信模板页、发送日志页和对应增量 SQL，让现有后端 `SMS` 能力形成菜单、页面、权限和接口的完整闭环。

**Architecture:** 保持现有后端菜单加载、动态路由映射和 `module-bridge` 兜底逻辑不变，只在后端声明的组件路径下新增真实页面文件。前端按现有 `api/system/*.ts` + `views/support/*` 模式落地，短信模板页承担查询/新增/编辑/启停，发送日志页承担只读审计查询，菜单和按钮权限通过单个增量 SQL 文件归位。

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc, pnpm, MySQL SQL

## Global Constraints

- 遵循 `AGENTS.md`：一次只推进一个可验证增量。
- 遵循 `AGENTS.md`：编辑前先说明为什么需要改动。
- 遵循 `AGENTS.md`：优先复用现有项目模式，不新增依赖。
- 所有前端页面遵循 `docs/frontend-list-table-page-standard.md`。
- 菜单路径和组件路径必须与后端菜单配置一致，不自造额外路径。
- 所有新增或编辑文本文件使用 UTF-8。
- 菜单图标使用 Element Plus 图标集的 Iconify 名称，如 `ep:chat-dot-round`。
- 若字段没有真实字典依赖，则本次不新增字典 SQL。
- 本次不新增真实短信供应商适配器。
- 本次不新增供应商回执、自动重试、失败补发。
- 本次不新增短信发送前端操作台。
- 本次不改造短信底层服务接口。
- 如需代码注释，使用简洁中文注释，且仅在逻辑不自解释处添加。
- 模板编辑态禁止修改 `templateCode`，避免与后端 `SmsService.updateTemplate()` 的主键更新语义冲突。
- 发送日志查询字段固定使用 `startDate`、`endDate`，与后端 `SmsSendLogQueryForm` 保持一致。
- 本次实现完成后必须验证 `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`。
- 本次实现完成后必须验证 `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck`。

---

## File Structure

### Contract and Source Tests

- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
  - 为 `sms.ts`、`template-list.vue`、`send-log-list.vue` 增加源码契约断言。

### API Module

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/sms.ts`
  - 定义短信模板、发送日志的类型、payload builder、启停路径 builder 和请求函数。
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/sms.test.ts`
  - 覆盖查询 payload、增改 payload、启停 URL builder、日志查询 payload。

### Page Modules

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/sms/template-list.vue`
  - 短信模板列表页，包含搜索、列表、弹窗表单、启停操作。
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/sms/send-log-list.vue`
  - 发送日志列表页，包含多条件搜索和状态中文映射。

### SQL

- Create: `数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql`
  - 新增父菜单 `短信管理`、子菜单 `短信模板` / `发送日志`，并将 `301-304` 号按钮权限归位。

## Task 1: Land the SMS API Module and Its Guardrail Tests

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/sms.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/sms.test.ts`

**Interfaces:**
- Consumes:
  - `requestClient` from `#/api/request`
  - `describe`, `it`, `expect` from `vitest`
  - `existsSync`, `readFileSync` from `node:fs`
  - `resolve` from `node:path`
- Produces:
  - `buildSmsTemplateQueryPayload(params: SmsTemplatePageQueryParams)`
  - `buildSmsTemplateMutationPayload<T extends SmsTemplateAddForm | SmsTemplateUpdateForm>(params: T): T`
  - `buildSmsTemplateDisabledPath(templateCode: string, disableFlag: boolean): string`
  - `buildSmsSendLogQueryPayload(params: SmsSendLogPageQueryParams)`
  - `querySmsTemplatePage(params: SmsTemplatePageQueryParams)`
  - `addSmsTemplate(params: SmsTemplateAddForm)`
  - `updateSmsTemplate(params: SmsTemplateUpdateForm)`
  - `updateSmsTemplateDisabled(templateCode: string, disableFlag: boolean)`
  - `querySmsSendLogPage(params: SmsSendLogPageQueryParams)`

- [ ] **Step 1: Extend the support-module contract with the SMS API assertions**

Append these constants near the top of `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`:

```ts
const smsTemplatePagePath =
  'apps/hunyuan-system/src/views/support/sms/template-list.vue';
const smsSendLogPagePath =
  'apps/hunyuan-system/src/views/support/sms/send-log-list.vue';
const smsApiPath = 'apps/hunyuan-system/src/api/system/sms.ts';
```

Add this API contract test inside the existing `describe('system settings support modules', ...)` block:

```ts
it('wires the sms api module to the backend sms endpoints', () => {
  const apiPath = resolve(process.cwd(), smsApiPath);

  expect(existsSync(apiPath)).toBe(true);

  const source = readFileSync(apiPath, 'utf8');
  expect(source).toContain("'/sms/template/query'");
  expect(source).toContain("'/sms/template/add'");
  expect(source).toContain("'/sms/template/update'");
  expect(source).toContain('/sms/template/updateDisabled/${encodeURIComponent(');
  expect(source).toContain("'/sms/sendLog/query'");
  expect(source).toContain('buildSmsTemplateQueryPayload');
  expect(source).toContain('buildSmsTemplateMutationPayload');
  expect(source).toContain('buildSmsTemplateDisabledPath');
  expect(source).toContain('buildSmsSendLogQueryPayload');
});
```

- [ ] **Step 2: Create the failing SMS API payload tests**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/sms.test.ts`:

```ts
import { describe, expect, it } from 'vitest';

import {
  buildSmsSendLogQueryPayload,
  buildSmsTemplateDisabledPath,
  buildSmsTemplateMutationPayload,
  buildSmsTemplateQueryPayload,
} from './sms';

describe('sms api payloads', () => {
  it('trims sms template query fields and preserves disableFlag filters', () => {
    expect(
      buildSmsTemplateQueryPayload({
        disableFlag: false,
        pageNum: 2,
        pageSize: 20,
        templateCode: '  login_code  ',
        templateName: '  登录验证码  ',
      }),
    ).toEqual({
      disableFlag: false,
      pageNum: 2,
      pageSize: 20,
      templateCode: 'login_code',
      templateName: '登录验证码',
    });
  });

  it('trims sms template mutation fields and preserves disableFlag on update', () => {
    expect(
      buildSmsTemplateMutationPayload({
        disableFlag: true,
        remark: '  登录场景模板  ',
        templateCode: '  login_code  ',
        templateContent: '  您的验证码是 ${code}  ',
        templateName: '  登录验证码  ',
      }),
    ).toEqual({
      disableFlag: true,
      remark: '登录场景模板',
      templateCode: 'login_code',
      templateContent: '您的验证码是 ${code}',
      templateName: '登录验证码',
    });
  });

  it('builds the sms template disabled path with encoded templateCode', () => {
    expect(
      buildSmsTemplateDisabledPath('  login code/test  ', true),
    ).toBe('/sms/template/updateDisabled/login%20code%2Ftest/true');
  });

  it('trims sms send-log query fields and preserves status and date filters', () => {
    expect(
      buildSmsSendLogQueryPayload({
        endDate: ' 2026-07-05 ',
        pageNum: 1,
        pageSize: 10,
        phone: ' 13800138000 ',
        sendStatus: 2,
        startDate: ' 2026-07-01 ',
        templateCode: ' login_code ',
      }),
    ).toEqual({
      endDate: '2026-07-05',
      pageNum: 1,
      pageSize: 10,
      phone: '13800138000',
      sendStatus: 2,
      startDate: '2026-07-01',
      templateCode: 'login_code',
    });
  });
});
```

- [ ] **Step 3: Run the focused tests and confirm they fail before implementation**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/sms.test.ts
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom -t "wires the sms api module to the backend sms endpoints"
```

Expected:
- First command fails with `Cannot find module './sms'` or missing file.
- Second command fails because `apps/hunyuan-system/src/api/system/sms.ts` does not exist yet.

- [ ] **Step 4: Implement the SMS API module**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/sms.ts`:

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

export interface SmsTemplateRecord {
  createTime?: null | string;
  disableFlag?: boolean;
  remark?: null | string;
  templateCode: string;
  templateContent: string;
  templateName: string;
  updateTime?: null | string;
}

export interface SmsSendLogRecord {
  createTime?: null | string;
  failReason?: null | string;
  phone: string;
  provider?: null | string;
  requestId?: null | string;
  sendContent: string;
  sendStatus?: null | number;
  sendTime?: null | string;
  smsSendLogId: number;
  templateCode: string;
}

export interface SmsTemplatePageQueryParams {
  disableFlag?: boolean;
  pageNum: number;
  pageSize: number;
  templateCode?: null | string;
  templateName?: null | string;
}

export interface SmsTemplateAddForm {
  disableFlag?: boolean;
  remark?: null | string;
  templateCode: string;
  templateContent: string;
  templateName: string;
}

export interface SmsTemplateUpdateForm extends SmsTemplateAddForm {}

export interface SmsSendLogPageQueryParams {
  endDate?: null | string;
  pageNum: number;
  pageSize: number;
  phone?: null | string;
  sendStatus?: null | number;
  startDate?: null | string;
  templateCode?: null | string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildSmsTemplateQueryPayload(
  params: SmsTemplatePageQueryParams,
) {
  return {
    disableFlag: params.disableFlag,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    templateCode: cleanText(params.templateCode) || undefined,
    templateName: cleanText(params.templateName) || undefined,
  };
}

export function buildSmsTemplateMutationPayload<
  T extends SmsTemplateAddForm | SmsTemplateUpdateForm,
>(params: T): T {
  return {
    ...params,
    disableFlag: params.disableFlag ?? false,
    remark: cleanText(params.remark) || undefined,
    templateCode: params.templateCode.trim(),
    templateContent: params.templateContent.trim(),
    templateName: params.templateName.trim(),
  };
}

export function buildSmsTemplateDisabledPath(
  templateCode: string,
  disableFlag: boolean,
) {
  return `/sms/template/updateDisabled/${encodeURIComponent(templateCode.trim())}/${disableFlag}`;
}

export function buildSmsSendLogQueryPayload(params: SmsSendLogPageQueryParams) {
  return {
    endDate: cleanText(params.endDate) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    phone: cleanText(params.phone) || undefined,
    sendStatus: params.sendStatus,
    startDate: cleanText(params.startDate) || undefined,
    templateCode: cleanText(params.templateCode) || undefined,
  };
}

export async function querySmsTemplatePage(params: SmsTemplatePageQueryParams) {
  return requestClient.post<PageResult<SmsTemplateRecord>>(
    '/sms/template/query',
    buildSmsTemplateQueryPayload(params),
  );
}

export async function addSmsTemplate(params: SmsTemplateAddForm) {
  return requestClient.post<string>(
    '/sms/template/add',
    buildSmsTemplateMutationPayload(params),
  );
}

export async function updateSmsTemplate(params: SmsTemplateUpdateForm) {
  return requestClient.post<string>(
    '/sms/template/update',
    buildSmsTemplateMutationPayload(params),
  );
}

export async function updateSmsTemplateDisabled(
  templateCode: string,
  disableFlag: boolean,
) {
  return requestClient.get<string>(
    buildSmsTemplateDisabledPath(templateCode, disableFlag),
  );
}

export async function querySmsSendLogPage(params: SmsSendLogPageQueryParams) {
  return requestClient.post<PageResult<SmsSendLogRecord>>(
    '/sms/sendLog/query',
    buildSmsSendLogQueryPayload(params),
  );
}
```

- [ ] **Step 5: Re-run the focused tests and confirm the API contract passes**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/sms.test.ts
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom -t "wires the sms api module to the backend sms endpoints"
```

Expected:
- `sms.test.ts` passes all 4 tests.
- The targeted support-module contract test passes and reports the five backend SMS endpoints plus the four builder names.

- [ ] **Step 6: Commit the API slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/sms.ts hunyuan-design/apps/hunyuan-system/src/api/system/sms.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts
git commit -m "feat: add sms api module"
```

Expected:
- Git creates one commit containing only the API file, its unit test, and the contract-test extension.

## Task 2: Implement the SMS Template Management Page

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/sms/template-list.vue`

**Interfaces:**
- Consumes:
  - `SmsTemplateAddForm`, `SmsTemplateRecord`, `SmsTemplateUpdateForm` from `#/api/system/sms`
  - `addSmsTemplate`, `querySmsTemplatePage`, `updateSmsTemplate`, `updateSmsTemplateDisabled` from `#/api/system/sms`
  - `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, `ArtTable`, `useTableColumns`
  - `Page` from `@vben/common-ui`
- Produces:
  - Page component `SystemSupportSmsTemplateList`
  - Row-action flow for `编辑` and `启用/停用`
  - Modal form model `SmsTemplateFormModel`

- [ ] **Step 1: Add the failing template-page contract tests**

Append these test cases to `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`:

```ts
it('provides a real sms template page at the backend-defined component path', () => {
  const pagePath = resolve(process.cwd(), smsTemplatePagePath);

  expect(existsSync(pagePath)).toBe(true);

  const source = readFileSync(pagePath, 'utf8');
  expect(source).toContain('SystemSupportSmsTemplateList');
  expect(source).toContain('ArtSearchPanel');
  expect(source).toContain('ArtTablePanel');
  expect(source).toContain('ArtTableHeader');
  expect(source).toContain('ArtTable');
});

it('keeps the sms template page dense and single-row search only', () => {
  const source = readFileSync(resolve(process.cwd(), smsTemplatePagePath), 'utf8');

  expect(source).toContain(':collapsible="false"');
  expect(source).not.toContain('template-page__title');
  expect(source).not.toContain('template-page__hero');
  expect(source).not.toContain('template-page__desc');
});

it('surfaces sms template query and mutation fields on the page', () => {
  const source = readFileSync(resolve(process.cwd(), smsTemplatePagePath), 'utf8');

  expect(source).toContain('templateCode');
  expect(source).toContain('templateName');
  expect(source).toContain('templateContent');
  expect(source).toContain('disableFlag');
  expect(source).toContain('remark');
  expect(source).toContain('新增模板');
});
```

- [ ] **Step 2: Run the targeted contract tests and confirm they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom -t "sms template page"
```

Expected:
- The run fails because `apps/hunyuan-system/src/views/support/sms/template-list.vue` does not exist yet.

- [ ] **Step 3: Implement the template management page**

Create `hunyuan-design/apps/hunyuan-system/src/views/support/sms/template-list.vue`:

```vue
<script setup lang="ts">
import type {
  SmsTemplateAddForm,
  SmsTemplateRecord,
  SmsTemplateUpdateForm,
} from '#/api/system/sms';
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
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addSmsTemplate,
  querySmsTemplatePage,
  updateSmsTemplate,
  updateSmsTemplateDisabled,
} from '#/api/system/sms';

defineOptions({ name: 'SystemSupportSmsTemplateList' });

interface SmsTemplateFormModel extends SmsTemplateAddForm {
  templateCode: string;
}

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<SmsTemplateRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  disableFlag: undefined as boolean | undefined,
  templateCode: '',
  templateName: '',
});

const formData = reactive<SmsTemplateFormModel>({
  disableFlag: false,
  remark: '',
  templateCode: '',
  templateContent: '',
  templateName: '',
});

const rules: FormRules<SmsTemplateFormModel> = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateContent: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<SmsTemplateRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'templateCode', label: '模板编码', minWidth: 180 },
  { prop: 'templateName', label: '模板名称', minWidth: 180 },
  {
    prop: 'templateContent',
    label: '模板内容',
    minWidth: 320,
    formatter: (row) => row.templateContent || '-',
  },
  {
    prop: 'disableFlag',
    label: '状态',
    width: 90,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 180,
    formatter: (row) => row.remark || '-',
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 160,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const dialogTitle = computed(() =>
  dialogMode.value === 'add' ? '新增短信模板' : '编辑短信模板',
);
const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resetForm() {
  Object.assign(formData, {
    disableFlag: false,
    remark: '',
    templateCode: '',
    templateContent: '',
    templateName: '',
  });
}

async function loadData() {
  loading.value = true;
  try {
    const result = await querySmsTemplatePage({
      disableFlag: searchForm.disableFlag,
      pageNum: pagination.current,
      pageSize: pagination.size,
      templateCode: searchForm.templateCode,
      templateName: searchForm.templateName,
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
  Object.assign(searchForm, {
    disableFlag: undefined,
    templateCode: '',
    templateName: '',
  });
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

function openEditDialog(row: SmsTemplateRecord) {
  dialogMode.value = 'edit';
  Object.assign(formData, {
    disableFlag: row.disableFlag ?? false,
    remark: row.remark || '',
    templateCode: row.templateCode,
    templateContent: row.templateContent,
    templateName: row.templateName,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addSmsTemplate(formData as SmsTemplateAddForm);
    ElMessage.success('新增短信模板成功');
  } else {
    await updateSmsTemplate(formData as SmsTemplateUpdateForm);
    ElMessage.success('更新短信模板成功');
  }

  dialogVisible.value = false;
  await loadData();
}

async function handleToggleDisabled(row: SmsTemplateRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.disableFlag ? '启用' : '停用'}模板“${row.templateName}”吗？`,
      '状态确认',
      { type: 'warning' },
    );
    await updateSmsTemplateDisabled(row.templateCode, !row.disableFlag);
    ElMessage.success('模板状态已更新');
    await loadData();
  } catch {
    // 用户取消
  }
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
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="template-page">
      <ElCard
        v-show="showSearchBar"
        class="template-page__search-card"
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
          <ElFormItem class="template-page__code-item" label="模板编码">
            <ElInput
              v-model="searchForm.templateCode"
              clearable
              placeholder="请输入模板编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="template-page__name-item" label="模板名称">
            <ElInput
              v-model="searchForm.templateName"
              clearable
              placeholder="请输入模板名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="template-page__status-item" label="状态">
            <ElSelect
              v-model="searchForm.disableFlag"
              clearable
              placeholder="请选择状态"
            >
              <ElOption :value="false" label="启用" />
              <ElOption :value="true" label="禁用" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="template-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">新增模板</ElButton>
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
            row-key="templateCode"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #disableFlag="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="row.disableFlag ? 'danger' : 'success'"
              >
                {{ row.disableFlag ? '禁用' : '启用' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="template-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openEditDialog(row)"
                >
                  编辑
                </ElButton>
                <ElButton
                  link
                  size="small"
                  :type="row.disableFlag ? 'success' : 'warning'"
                  @click="handleToggleDisabled(row)"
                >
                  {{ row.disableFlag ? '启用' : '停用' }}
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <ElDialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="680px"
        @closed="resetForm"
      >
        <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
          <ElFormItem label="模板编码" prop="templateCode">
            <ElInput
              v-model="formData.templateCode"
              :disabled="dialogMode === 'edit'"
              placeholder="请输入模板编码"
            />
          </ElFormItem>
          <ElFormItem label="模板名称" prop="templateName">
            <ElInput v-model="formData.templateName" placeholder="请输入模板名称" />
          </ElFormItem>
          <ElFormItem label="模板内容" prop="templateContent">
            <ElInput
              v-model="formData.templateContent"
              :rows="5"
              placeholder="请输入模板内容"
              type="textarea"
            />
          </ElFormItem>
          <ElFormItem label="是否禁用" prop="disableFlag">
            <ElSwitch
              v-model="formData.disableFlag"
              inline-prompt
              active-text="禁用"
              inactive-text="启用"
            />
          </ElFormItem>
          <ElFormItem label="备注" prop="remark">
            <ElInput
              v-model="formData.remark"
              :rows="4"
              maxlength="500"
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
    </div>
  </Page>
</template>

<style scoped>
.template-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.template-page__search-card,
.template-page__table-card {
  border-radius: 8px;
}

.template-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.template-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.template-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.template-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.template-page :deep(.art-table-panel),
.template-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.template-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.template-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.template-page__code-item :deep(.el-form-item__content),
.template-page__name-item :deep(.el-form-item__content) {
  width: 220px;
}

.template-page__status-item :deep(.el-form-item__content) {
  width: 168px;
}

.template-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.template-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.template-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .template-page__code-item :deep(.el-form-item__content),
  .template-page__name-item :deep(.el-form-item__content),
  .template-page__status-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
```

- [ ] **Step 4: Verify the template page contract and type safety**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom -t "sms template page"
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The three template-page contract tests pass.
- `@hunyuan/system` typecheck passes with the new page importing `#/api/system/sms`.

- [ ] **Step 5: Commit the template page slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/sms/template-list.vue
git commit -m "feat: add sms template management page"
```

Expected:
- Git creates one commit containing only the template page and its associated source-contract assertions.

## Task 3: Implement the SMS Send-Log Page

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/sms/send-log-list.vue`

**Interfaces:**
- Consumes:
  - `SmsSendLogRecord` from `#/api/system/sms`
  - `querySmsSendLogPage` from `#/api/system/sms`
  - `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, `ArtTable`, `useTableColumns`
  - `Page` from `@vben/common-ui`
- Produces:
  - Page component `SystemSupportSmsSendLogList`
  - Search model with `phone`, `templateCode`, `sendStatus`, `startDate`, `endDate`
  - Local status label and tag helpers for `0/1/2`

- [ ] **Step 1: Add the failing send-log page contract tests**

Append these test cases to `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`:

```ts
it('provides a real sms send-log page at the backend-defined component path', () => {
  const pagePath = resolve(process.cwd(), smsSendLogPagePath);

  expect(existsSync(pagePath)).toBe(true);

  const source = readFileSync(pagePath, 'utf8');
  expect(source).toContain('SystemSupportSmsSendLogList');
  expect(source).toContain('ArtSearchPanel');
  expect(source).toContain('ArtTablePanel');
  expect(source).toContain('ArtTableHeader');
  expect(source).toContain('ArtTable');
});

it('keeps the sms send-log page dense and preserves collapsible multi-filter search', () => {
  const source = readFileSync(resolve(process.cwd(), smsSendLogPagePath), 'utf8');

  expect(source).not.toContain(':collapsible="false"');
  expect(source).not.toContain('send-log-page__title');
  expect(source).not.toContain('send-log-page__hero');
  expect(source).not.toContain('send-log-page__desc');
});

it('surfaces sms send-log filter and table fields on the page', () => {
  const source = readFileSync(resolve(process.cwd(), smsSendLogPagePath), 'utf8');

  expect(source).toContain('phone');
  expect(source).toContain('templateCode');
  expect(source).toContain('sendStatus');
  expect(source).toContain('startDate');
  expect(source).toContain('endDate');
  expect(source).toContain('provider');
  expect(source).toContain('requestId');
  expect(source).toContain('sendContent');
  expect(source).toContain('failReason');
});
```

- [ ] **Step 2: Run the targeted contract tests and confirm they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom -t "sms send-log page"
```

Expected:
- The run fails because `apps/hunyuan-system/src/views/support/sms/send-log-list.vue` does not exist yet.

- [ ] **Step 3: Implement the send-log page**

Create `hunyuan-design/apps/hunyuan-system/src/views/support/sms/send-log-list.vue`:

```vue
<script setup lang="ts">
import type { SmsSendLogRecord } from '#/api/system/sms';
import type { ColumnOption } from '@vben/art-hooks/table';

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
  ElCard,
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import { querySmsSendLogPage } from '#/api/system/sms';

defineOptions({ name: 'SystemSupportSmsSendLogList' });

const sendStatusOptions = [
  { label: '待发送', value: 0 },
  { label: '发送成功', value: 1 },
  { label: '发送失败', value: 2 },
] as const;

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<SmsSendLogRecord[]>([]);

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  endDate: '',
  phone: '',
  sendStatus: undefined as number | undefined,
  startDate: '',
  templateCode: '',
});

const columnsFactory = (): ColumnOption<SmsSendLogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'provider',
    label: '供应商',
    minWidth: 120,
    formatter: (row) => row.provider || '-',
  },
  {
    prop: 'requestId',
    label: '请求单号',
    minWidth: 180,
    formatter: (row) => row.requestId || '-',
  },
  { prop: 'phone', label: '手机号', minWidth: 140 },
  { prop: 'templateCode', label: '模板编码', minWidth: 180 },
  {
    prop: 'sendContent',
    label: '发送内容',
    minWidth: 320,
    formatter: (row) => row.sendContent || '-',
  },
  {
    prop: 'sendStatus',
    label: '发送状态',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'failReason',
    label: '失败原因',
    minWidth: 220,
    formatter: (row) => row.failReason || '-',
  },
  {
    prop: 'sendTime',
    label: '发送时间',
    minWidth: 180,
    formatter: (row) => row.sendTime || '-',
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 180,
    formatter: (row) => row.createTime || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resolveSendStatusLabel(value?: null | number) {
  return sendStatusOptions.find((item) => item.value === value)?.label || '-';
}

function resolveSendStatusType(value?: null | number) {
  if (value === 1) {
    return 'success';
  }
  if (value === 2) {
    return 'danger';
  }
  return 'warning';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await querySmsSendLogPage({
      endDate: searchForm.endDate,
      pageNum: pagination.current,
      pageSize: pagination.size,
      phone: searchForm.phone,
      sendStatus: searchForm.sendStatus,
      startDate: searchForm.startDate,
      templateCode: searchForm.templateCode,
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
  Object.assign(searchForm, {
    endDate: '',
    phone: '',
    sendStatus: undefined,
    startDate: '',
    templateCode: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
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
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="send-log-page">
      <ElCard
        v-show="showSearchBar"
        class="send-log-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="send-log-page__phone-item" label="手机号">
            <ElInput
              v-model="searchForm.phone"
              clearable
              placeholder="请输入手机号"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="send-log-page__template-item" label="模板编码">
            <ElInput
              v-model="searchForm.templateCode"
              clearable
              placeholder="请输入模板编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="send-log-page__status-item" label="发送状态">
            <ElSelect
              v-model="searchForm.sendStatus"
              clearable
              placeholder="请选择发送状态"
            >
              <ElOption
                v-for="item in sendStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="send-log-page__date-item" label="开始日期">
            <ElDatePicker
              v-model="searchForm.startDate"
              placeholder="请选择开始日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
          <ElFormItem class="send-log-page__date-item" label="结束日期">
            <ElDatePicker
              v-model="searchForm.endDate"
              placeholder="请选择结束日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="send-log-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          />

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
            row-key="smsSendLogId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #sendStatus="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="resolveSendStatusType(row.sendStatus)"
              >
                {{ resolveSendStatusLabel(row.sendStatus) }}
              </ElTag>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.send-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.send-log-page__search-card,
.send-log-page__table-card {
  border-radius: 8px;
}

.send-log-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.send-log-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.send-log-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.send-log-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.send-log-page :deep(.art-table-panel),
.send-log-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.send-log-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.send-log-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.send-log-page__phone-item :deep(.el-form-item__content),
.send-log-page__template-item :deep(.el-form-item__content) {
  width: 220px;
}

.send-log-page__status-item :deep(.el-form-item__content),
.send-log-page__date-item :deep(.el-form-item__content) {
  width: 168px;
}

@media (width <= 768px) {
  .send-log-page__phone-item :deep(.el-form-item__content),
  .send-log-page__template-item :deep(.el-form-item__content),
  .send-log-page__status-item :deep(.el-form-item__content),
  .send-log-page__date-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
```

- [ ] **Step 4: Verify the send-log page contract and type safety**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom -t "sms send-log page"
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The three send-log page contract tests pass.
- `@hunyuan/system` typecheck still passes after adding `ElDatePicker`, `ElTag`, and the log-page search model.

- [ ] **Step 5: Commit the send-log page slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/sms/send-log-list.vue
git commit -m "feat: add sms send log page"
```

Expected:
- Git creates one commit containing only the send-log page and its associated source-contract assertions.

## Task 4: Add the Incremental Menu SQL and Run Full Acceptance Verification

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql`
- Verify: `hunyuan-design/apps/hunyuan-system/src/api/system/sms.test.ts`
- Verify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Verify: `hunyuan-design/apps/hunyuan-system/src/views/support/sms/template-list.vue`
- Verify: `hunyuan-design/apps/hunyuan-system/src/views/support/sms/send-log-list.vue`

**Interfaces:**
- Consumes:
  - Existing button permission rows `301`, `302`, `303`, `304` from `数据库SQL脚本/mysql/sql-update-log/v3.31.0.sql`
  - Backend menu parent `50` for system settings
- Produces:
  - Menu rows `305` = `短信管理`, `306` = `短信模板`, `307` = `发送日志`
  - Button rows `301-303` re-parented to `306`
  - Button row `304` re-parented to `307`

- [ ] **Step 1: Create the incremental menu SQL**

Create `数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql`:

```sql
INSERT INTO `t_menu` (
  `menu_id`,
  `menu_name`,
  `menu_type`,
  `parent_id`,
  `sort`,
  `path`,
  `component`,
  `perms_type`,
  `api_perms`,
  `web_perms`,
  `icon`,
  `context_menu_id`,
  `frame_flag`,
  `frame_url`,
  `cache_flag`,
  `visible_flag`,
  `disabled_flag`,
  `deleted_flag`,
  `create_user_id`,
  `create_time`,
  `update_user_id`,
  `update_time`
)
VALUES
  (305, '短信管理', 1, 50, 31, '/support/sms', NULL, 1, NULL, NULL, 'ep:chat-dot-round', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (306, '短信模板', 2, 305, 1, '/support/sms/template-list', '/support/sms/template-list.vue', 1, NULL, NULL, 'ep:tickets', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (307, '发送日志', 2, 305, 2, '/support/sms/send-log-list', '/support/sms/send-log-list.vue', 1, NULL, NULL, 'ep:list', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`),
  `sort` = VALUES(`sort`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `update_time` = now();

UPDATE `t_menu`
SET
  `parent_id` = 306,
  `context_menu_id` = 306,
  `update_time` = now()
WHERE `menu_id` IN (301, 302, 303);

UPDATE `t_menu`
SET
  `parent_id` = 307,
  `context_menu_id` = 307,
  `update_time` = now()
WHERE `menu_id` = 304;
```

- [ ] **Step 2: Verify the SQL shape and guard the “no dictionary SQL” boundary**

Run:

```bash
rg -n "305|306|307|短信管理|短信模板|发送日志|ep:chat-dot-round|ep:tickets|ep:list|context_menu_id" 数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql
rg -n "dict|字典" 数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql
```

Expected:
- First command prints the three menu ids, three menu names, three icon strings, and the `context_menu_id` updates.
- Second command prints no matches, confirming this increment does not add dictionary SQL.

- [ ] **Step 3: Run the focused API and source contract suite**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/sms.test.ts
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
```

Expected:
- `sms.test.ts` passes all payload-builder tests.
- `system-settings-modules.test.ts` passes the existing support-module contracts plus the new SMS API / page assertions.

- [ ] **Step 4: Run the full frontend type checks required by the spec**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected:
- `@hunyuan/system` typecheck passes.
- `@vben/web-ele` typecheck passes, satisfying the shared list-page contract gate from repo guidance.

- [ ] **Step 5: Commit the SQL and verified integration slice**

Run:

```bash
git add 数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql
git commit -m "feat: add sms management menu sql"
```

Expected:
- Git creates one SQL-focused commit for `v3.32.0.sql`; API 和页面文件已经在 Task 1-3 的小步提交中落库。

## Self-Review Checklist

### Spec Coverage

- 短信模板页：Task 2 覆盖查询、新增、编辑、启停。
- 发送日志页：Task 3 覆盖手机号、模板编码、发送状态、日期范围查询和本地中文状态映射。
- 增量 SQL：Task 4 覆盖父菜单、两个子菜单、按钮权限归位。
- 图标：Task 4 固定为 `ep:chat-dot-round`、`ep:tickets`、`ep:list`。
- 字典 SQL 边界：Task 4 明确验证 `v3.32.0.sql` 不包含字典增量。
- UTF-8 / 中文文案 / 中文注释约束：Global Constraints 已固定。

### Placeholder Scan

- 计划中没有 `TODO`、`TBD`、`implement later`、`similar to Task N` 之类占位符。
- 每个新文件都给出了确切路径、确切命令和可直接落地的代码骨架。

### Type Consistency

- `sms.ts` 的函数名、参数名、页面消费名在 Task 1、Task 2、Task 3 中保持一致。
- `startDate` / `endDate`、`disableFlag` / `sendStatus` 在 API、页面、测试和 SQL 说明中使用同一命名。
- 页面组件名固定为 `SystemSupportSmsTemplateList` 和 `SystemSupportSmsSendLogList`，与源码契约测试名称一致。
