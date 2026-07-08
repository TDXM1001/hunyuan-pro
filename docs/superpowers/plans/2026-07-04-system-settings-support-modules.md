# System Settings Support Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the remaining system-settings `module-bridge` entries with real backend-wired pages for file, message, job, serial-number, cache, and reload management on the current branch.

**Architecture:** Keep backend menu loading, `login-adapter.ts`, and `module-bridge` fallback unchanged. Add one focused `api/system/*.ts` module per backend capability, land page files exactly at the backend-declared component paths under `views/support/*`, and split secondary data into page-local drawers instead of inventing new routes or shared abstractions.

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc, pnpm

## Global Constraints

- Follow `AGENTS.md`: make one incremental change at a time.
- Follow `AGENTS.md`: explain why a change is needed before editing files.
- Follow `AGENTS.md`: prefer existing project patterns over new abstractions.
- Follow `AGENTS.md`: do not add new dependencies without explicit approval.
- Follow `AGENTS.md`: ordinary menu-backed list pages must stay quiet and dense, without extra hero/title/desc copy.
- Follow `docs/frontend-list-table-page-standard.md` for all list/search/table pages.
- Strictly use the backend menu component paths from `t_menu`; do not invent different page paths.
- Keep request logic in `hunyuan-design/apps/hunyuan-system/src/api/system/*.ts`, not in shared `@vben/art-hooks` components.
- Keep login, backend menu loading, dynamic route mapping, and `module-bridge` behavior unchanged.
- Implement on the current branch `main` because the user explicitly approved current-branch execution.
- Use UTF-8 for reads and writes.
- Use concise Chinese code comments only where logic is not self-explanatory.
- Verify meaningful frontend changes with `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck`.

---

## File Structure

### Shared Contract and Source Tests

- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
  - Extend source contracts from `config` and `dict` to the remaining six support modules.

### API Payload and Endpoint Modules

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/file.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/file.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/message.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/message.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/job.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/job.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/serial-number.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/serial-number.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/cache.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/cache.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/reload.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/reload.test.ts`

### Page Modules

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/file/file-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/message/message-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/job/job-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/job/components/job-log-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/serial-number/serial-number-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/serial-number/components/serial-number-record-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/cache/cache-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/cache/components/cache-key-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/reload/reload-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/reload/components/reload-result-drawer.vue`

## Task 1: Lock the Remaining System-Settings Module Contracts

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/file.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/message.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/job.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/serial-number.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/cache.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/reload.test.ts`

**Interfaces:**
- Consumes:
  - `describe`, `it`, `expect` from `vitest`
  - `existsSync`, `readFileSync` from `node:fs`
  - `resolve` from `node:path`
- Produces:
  - Source-level contracts for the six real page files.
  - Payload-builder contracts for each new `api/system/*.ts` module.

- [ ] **Step 1: Extend the support-module source contract**

Append page-path constants in `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`:

```ts
const filePagePath = 'apps/hunyuan-system/src/views/support/file/file-list.vue';
const messagePagePath = 'apps/hunyuan-system/src/views/support/message/message-list.vue';
const jobPagePath = 'apps/hunyuan-system/src/views/support/job/job-list.vue';
const jobDrawerPath =
  'apps/hunyuan-system/src/views/support/job/components/job-log-drawer.vue';
const serialNumberPagePath =
  'apps/hunyuan-system/src/views/support/serial-number/serial-number-list.vue';
const serialNumberDrawerPath =
  'apps/hunyuan-system/src/views/support/serial-number/components/serial-number-record-drawer.vue';
const cachePagePath = 'apps/hunyuan-system/src/views/support/cache/cache-list.vue';
const cacheDrawerPath =
  'apps/hunyuan-system/src/views/support/cache/components/cache-key-drawer.vue';
const reloadPagePath = 'apps/hunyuan-system/src/views/support/reload/reload-list.vue';
const reloadDrawerPath =
  'apps/hunyuan-system/src/views/support/reload/components/reload-result-drawer.vue';
```

Add source assertions that each page contains `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, `ArtTable`, and the expected page component name, and that each drawer contains the expected drawer component name.

- [ ] **Step 2: Create failing API payload tests**

Create one focused API payload test file per module. Each file should follow the existing `config.test.ts` / `dict.test.ts` style. For example, `message.test.ts` should assert:

```ts
expect(
  buildMessagePageQueryPayload({
    endDate: '2026-07-04',
    messageType: 1,
    pageNum: 2,
    pageSize: 20,
    readFlag: false,
    receiverUserId: 9,
    receiverUserType: 1,
    searchWord: '  系统通知  ',
    startDate: '2026-07-01',
  }),
).toEqual({
  endDate: '2026-07-04',
  messageType: 1,
  pageNum: 2,
  pageSize: 20,
  readFlag: false,
  receiverUserId: 9,
  receiverUserType: 1,
  searchWord: '系统通知',
  startDate: '2026-07-01',
});
```

`serial-number.test.ts` should assert `buildSerialNumberGeneratePayload({ serialNumberId: 1, count: 3 })`, `cache.test.ts` should assert `buildCacheNamePath('sys_cache')`, and `reload.test.ts` should assert `buildReloadMutationPayload({ tag: '  login-config  ', identification: '  20260704  ', args: '  force=true  ' })`.

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/file.test.ts apps/hunyuan-system/src/api/system/message.test.ts apps/hunyuan-system/src/api/system/job.test.ts apps/hunyuan-system/src/api/system/serial-number.test.ts apps/hunyuan-system/src/api/system/cache.test.ts apps/hunyuan-system/src/api/system/reload.test.ts --dom
```

Expected:

- The source contract fails because the six support page files do not exist yet.
- The API payload tests fail because the six new `api/system/*.ts` modules do not exist yet.

## Task 2: Add File and Message API Modules plus Standard Management Pages

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/file.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/message.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/file/file-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/message/message-list.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/file.test.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/message.test.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

**Interfaces:**
- Consumes:
  - `requestClient`, `baseRequestClient` from `#/api/request`
  - Existing list-page composition from `views/support/config/config-list.vue`
- Produces:
  - `buildFilePageQueryPayload(params: FilePageQueryParams)`
  - `queryFilePage(params: FilePageQueryParams)`
  - `getFilePreviewUrl(fileKey: string): string`
  - `getFileDownloadUrl(fileKey: string): string`
  - `buildMessagePageQueryPayload(params: MessagePageQueryParams)`
  - `buildMessageSendPayload(params: MessageSendFormModel)`
  - `queryMessagePage(params: MessagePageQueryParams)`
  - `sendMessage(params: MessageSendFormModel)`
  - `deleteMessage(messageId: number)`
  - `SystemSupportFileList`
  - `SystemSupportMessageList`

- [ ] **Step 1: Create `file.ts`**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/file.ts` with:

```ts
export interface FilePageQueryParams {
  createTimeBegin?: string;
  createTimeEnd?: string;
  creatorName?: string;
  fileKey?: string;
  fileName?: string;
  fileType?: string;
  folderType?: number;
  pageNum: number;
  pageSize: number;
}

export function buildFilePageQueryPayload(params: FilePageQueryParams) {
  return {
    createTimeBegin: cleanText(params.createTimeBegin) || undefined,
    createTimeEnd: cleanText(params.createTimeEnd) || undefined,
    creatorName: cleanText(params.creatorName) || undefined,
    fileKey: cleanText(params.fileKey) || undefined,
    fileName: cleanText(params.fileName) || undefined,
    fileType: cleanText(params.fileType) || undefined,
    folderType: params.folderType,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}
```

and expose preview/download helpers:

```ts
export function getFilePreviewUrl(fileKey: string) {
  return `${baseRequestClient.options.baseURL}/file/getFileUrl?fileKey=${encodeURIComponent(fileKey)}`;
}

export function getFileDownloadUrl(fileKey: string) {
  return `${baseRequestClient.options.baseURL}/file/downLoad?fileKey=${encodeURIComponent(fileKey)}`;
}
```

- [ ] **Step 2: Create `message.ts`**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/message.ts` with trimmed search payloads and single-row send payload normalization:

```ts
export interface MessageSendFormModel {
  content: string;
  dataId?: null | string;
  messageType: number;
  receiverUserId: number;
  receiverUserType: number;
  title: string;
}

export function buildMessageSendPayload(params: MessageSendFormModel) {
  return [
    {
      content: params.content.trim(),
      dataId: cleanText(params.dataId) || undefined,
      messageType: params.messageType,
      receiverUserId: params.receiverUserId,
      receiverUserType: params.receiverUserType,
      title: params.title.trim(),
    },
  ];
}
```

- [ ] **Step 3: Create `file-list.vue` and `message-list.vue`**

Create both pages by copying the dense list-page contract from `config-list.vue` and changing only the business fields and actions. Required behaviors:

```vue
<ArtSearchPanel :collapsible="false" ... />
<ArtTableHeader layout="search,size,fullscreen,columns,settings" ... />
<ArtTable :pagination-options="{ align: 'center', hideOnSinglePage: false, layout: 'sizes, prev, pager, next, jumper', pageSizes: [10, 20, 30], showTotalSummary: true, size: 'small' }" />
```

`file-list.vue` must provide row actions `查看链接` and `下载文件`. `message-list.vue` must provide a page-level `发送消息` button and row-level `删除` action.

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/file.test.ts apps/hunyuan-system/src/api/system/message.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected: all PASS, and source tests now recognize the file/message pages as real backend-defined page files.

## Task 3: Add Job API Module, Job Page, and Job Log Drawer

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/job.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/job/job-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/job/components/job-log-drawer.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/job.test.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

**Interfaces:**
- Consumes:
  - `SmartJobQueryForm`, `SmartJobAddForm`, `SmartJobUpdateForm`, `SmartJobEnabledUpdateForm`, `SmartJobLogQueryForm`, `SmartJobVO`, `SmartJobLogVO`
- Produces:
  - `buildJobPageQueryPayload`
  - `buildJobMutationPayload`
  - `buildJobEnabledPayload`
  - `buildJobLogQueryPayload`
  - `queryJobPage`
  - `addJob`
  - `updateJob`
  - `updateJobEnabled`
  - `executeJob`
  - `deleteJob`
  - `queryJobLogs`
  - `SystemSupportJobList`
  - `SystemSupportJobLogDrawer`

- [ ] **Step 1: Create `job.ts`**

Create payload builders that trim text fields and preserve booleans:

```ts
export function buildJobEnabledPayload(params: JobEnabledFormModel) {
  return {
    enabledFlag: params.enabledFlag,
    jobId: params.jobId,
  };
}

export function buildJobLogQueryPayload(params: JobLogQueryParams) {
  return {
    endTime: cleanText(params.endTime) || undefined,
    jobId: params.jobId,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    searchWord: cleanText(params.searchWord) || undefined,
    startTime: cleanText(params.startTime) || undefined,
    successFlag: params.successFlag,
  };
}
```

- [ ] **Step 2: Create `job-log-drawer.vue`**

Create a drawer component with component name `SystemSupportJobLogDrawer` that accepts:

```ts
defineProps<{
  job?: JobRecord;
}>();
```

and renders `ArtSearchPanel + ArtTable` for `POST /job/log/query`.

- [ ] **Step 3: Create `job-list.vue`**

Create `SystemSupportJobList` with search by `searchWord`, `triggerType`, `enabledFlag`, and page actions `新增任务`, row actions `编辑`, `启停`, `立即执行`, `执行日志`, `删除`. Keep add/edit inside a dialog; keep log history inside the drawer component.

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/job.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected: all PASS, and the job page plus log drawer satisfy the support-module source contract.

## Task 4: Add Serial-Number API Module, Page, and Record Drawer

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/serial-number.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/serial-number/serial-number-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/serial-number/components/serial-number-record-drawer.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/serial-number.test.ts`

**Interfaces:**
- Consumes:
  - `SerialNumberEntity`
  - `SerialNumberRecordEntity`
  - `SerialNumberRecordQueryForm`
  - `SerialNumberGenerateForm`
- Produces:
  - `querySerialNumberList()`
  - `buildSerialNumberRecordQueryPayload`
  - `querySerialNumberRecords`
  - `buildSerialNumberGeneratePayload`
  - `generateSerialNumbers`
  - `SystemSupportSerialNumberList`
  - `SystemSupportSerialNumberRecordDrawer`

- [ ] **Step 1: Create `serial-number.ts`**

Expose the record and generate builders:

```ts
export function buildSerialNumberRecordQueryPayload(params: SerialNumberRecordQueryParams) {
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    serialNumberId: params.serialNumberId,
  };
}

export function buildSerialNumberGeneratePayload(params: SerialNumberGenerateParams) {
  return {
    count: params.count,
    serialNumberId: params.serialNumberId,
  };
}
```

- [ ] **Step 2: Create drawer and page**

Create a list page that loads `GET /serialNumber/all`, renders business name, format, rule type, init/last number, and uses a right drawer for record history loaded from `POST /serialNumber/queryRecord`. The page must also include a small generate dialog:

```vue
<ElInputNumber v-model="generateForm.count" :min="1" :max="50" />
```

- [ ] **Step 3: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/serial-number.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected: all PASS.

## Task 5: Add Cache API Module, Page, and Cache-Key Drawer

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/cache.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/cache/cache-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/cache/components/cache-key-drawer.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/cache.test.ts`

**Interfaces:**
- Consumes:
  - `GET /cache/names`
  - `GET /cache/keys/{cacheName}`
  - `GET /cache/remove/{cacheName}`
- Produces:
  - `buildCacheKeysPath(cacheName: string)`
  - `buildCacheRemovePath(cacheName: string)`
  - `queryCacheNames()`
  - `queryCacheKeys(cacheName: string)`
  - `removeCache(cacheName: string)`
  - `SystemSupportCacheList`
  - `SystemSupportCacheKeyDrawer`

- [ ] **Step 1: Create `cache.ts`**

Create encoded path builders:

```ts
export function buildCacheKeysPath(cacheName: string) {
  return `/cache/keys/${encodeURIComponent(cacheName.trim())}`;
}

export function buildCacheRemovePath(cacheName: string) {
  return `/cache/remove/${encodeURIComponent(cacheName.trim())}`;
}
```

- [ ] **Step 2: Create drawer and page**

`cache-list.vue` should render one quiet list page with a single search input for cache name filtering on the frontend after `GET /cache/names`, row actions `查看 Keys` and `删除缓存`, and a drawer that loads `GET /cache/keys/{cacheName}`.

- [ ] **Step 3: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/cache.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected: all PASS.

## Task 6: Add Reload API Module, Page, and Result Drawer

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/reload.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/reload/reload-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/reload/components/reload-result-drawer.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/reload.test.ts`

**Interfaces:**
- Consumes:
  - `ReloadItemVO`
  - `ReloadResultVO`
  - `ReloadForm`
- Produces:
  - `buildReloadMutationPayload`
  - `queryReloadItems`
  - `updateReloadItem`
  - `queryReloadResults`
  - `SystemSupportReloadList`
  - `SystemSupportReloadResultDrawer`

- [ ] **Step 1: Create `reload.ts`**

Normalize mutation payloads:

```ts
export function buildReloadMutationPayload(params: ReloadFormModel) {
  return {
    args: cleanText(params.args) || undefined,
    identification: params.identification.trim(),
    tag: params.tag.trim(),
  };
}
```

- [ ] **Step 2: Create drawer and page**

The page should query `GET /reload/query`, show `tag / identification / args / updateTime`, and provide row actions `更新配置` and `结果历史`. `更新配置` uses a dialog and `结果历史` opens `reload-result-drawer.vue`.

- [ ] **Step 3: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/reload.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected: all PASS.

## Task 7: Final Support-Module Closure Verification

**Files:**
- Modify only if targeted verification reveals a direct issue:
  - `hunyuan-design/apps/hunyuan-system/src/api/system/*.ts`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/**/*.vue`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

**Interfaces:**
- Consumes:
  - The six new page files at the exact backend component paths
  - The six new `api/system/*.ts` modules
- Produces:
  - Verified closure for the remaining system-settings modules without changing routing infrastructure

- [ ] **Step 1: Run all targeted vitest contracts**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/file.test.ts apps/hunyuan-system/src/api/system/message.test.ts apps/hunyuan-system/src/api/system/job.test.ts apps/hunyuan-system/src/api/system/serial-number.test.ts apps/hunyuan-system/src/api/system/cache.test.ts apps/hunyuan-system/src/api/system/reload.test.ts apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 2: Run final typecheck**

Run:

```bash
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected: PASS.

- [ ] **Step 3: Verify route-matching assumptions remain unchanged**

Run:

```bash
rg -n "normalizeComponentPath|MODULE_BRIDGE_COMPONENT|/support/file/file-list.vue|/support/job/job-list.vue|/support/message/message-list.vue|/support/serial-number/serial-number-list.vue|/support/cache/cache-list.vue|/support/reload/reload-list.vue" hunyuan-design/apps/hunyuan-system/src
```

Expected:

- `login-adapter.ts` still owns route normalization.
- The six real support page files exist exactly at the backend-declared component paths.
- No changes are needed in `module-bridge/index.vue`.

## Self-Review

### Spec coverage

- The remaining six system-settings bridge pages are each mapped to a concrete task.
- Standard management pages (`file`, `message`, `job`) are separated from tool pages (`serial-number`, `cache`, `reload`) exactly as required by the spec.
- API modules remain under `apps/hunyuan-system/src/api/system/*.ts`.
- Secondary data stays in page-local drawers, preserving parent list context.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task includes explicit file paths and verification commands.

### Type consistency

- Each task defines the API builder and page component names it later consumes.
- Drawer component names match the source-contract tests:
  - `SystemSupportJobLogDrawer`
  - `SystemSupportSerialNumberRecordDrawer`
  - `SystemSupportCacheKeyDrawer`
  - `SystemSupportReloadResultDrawer`
