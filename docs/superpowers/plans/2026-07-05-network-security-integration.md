# Network Security Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the network-security `module-bridge` routes in `hunyuan-system` with real backend-wired pages that match current backend maturity: four management pages plus two first-phase capability pages.

**Architecture:** Keep backend menu loading, `login-adapter.ts`, and `module-bridge` fallback unchanged. Add one source-contract test dedicated to the network-security menu group, split frontend API modules by capability, and land page files exactly at the backend-defined component paths. For `接口加解密`, do not introduce a new frontend crypto dependency in phase one; only the response-encrypt endpoint is exercised live, while request-encrypt scenarios are surfaced as explicit sample-envelope contracts.

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc, pnpm

## Global Constraints

- Follow `AGENTS.md`: make one incremental change at a time.
- Follow `AGENTS.md`: explain why a change is needed before editing files.
- Follow `AGENTS.md`: prefer existing project patterns over new abstractions.
- Follow `AGENTS.md`: do not add new dependencies without explicit approval.
- List pages must follow `docs/frontend-list-table-page-standard.md`.
- Edit/detail/config pages must follow `docs/frontend-edit-detail-page-standard.md`.
- Page paths must exactly match backend `t_menu.component` values.
- Keep request logic in `hunyuan-design/apps/hunyuan-system/src/api/system/*.ts`.
- Keep `login-adapter.ts` and `module-bridge` unchanged.
- Use UTF-8 for reads and writes.
- First-phase `接口加解密` does not add a frontend crypto utility and does not import new crypto packages directly.

---

## File Structure

### Source Contract Tests

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
  - Locks the backend-defined page paths, page component names, and high-level page structure for the network-security menu group.

### API Modules

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.ts`
  - Owns level-3 protect config parsing/mutation plus login-fail list/delete requests.
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/login-log.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/login-log.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.test.ts`

### Page Modules

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/level3-protect-config-index.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/login-fail/login-fail-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/login-log/login-log-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/operate-log-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/components/operate-log-detail-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/api-encrypt/api-encrypt-index.vue`

## Task 1: Lock Network Security Route and API Contracts

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/login-log.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.test.ts`

**Interfaces:**
- Consumes:
  - Existing Vitest source-contract style from `apps/hunyuan-system/src/views/system/organization-modules.test.ts`
  - Existing payload-builder test style from `apps/hunyuan-system/src/api/system/config.test.ts`
- Produces:
  - `network-security-modules.test.ts` source contract for six real pages
  - Failing API payload tests for every new `api/system/*` module used later in the plan

- [ ] **Step 1: Write the failing source contract test**

```ts
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const level3ProtectPagePath =
  'apps/hunyuan-system/src/views/support/level3protect/level3-protect-config-index.vue';
const loginFailPagePath =
  'apps/hunyuan-system/src/views/support/login-fail/login-fail-list.vue';
const loginLogPagePath =
  'apps/hunyuan-system/src/views/support/login-log/login-log-list.vue';
const operateLogPagePath =
  'apps/hunyuan-system/src/views/support/operate-log/operate-log-list.vue';
const operateLogDrawerPath =
  'apps/hunyuan-system/src/views/support/operate-log/components/operate-log-detail-drawer.vue';
const dataMaskingPagePath =
  'apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue';
const apiEncryptPagePath =
  'apps/hunyuan-system/src/views/support/api-encrypt/api-encrypt-index.vue';

describe('network security backend menu docking pages', () => {
  it('provides a real level-3 protect config page', () => {
    const source = readFileSync(resolve(process.cwd(), level3ProtectPagePath), 'utf8');
    expect(existsSync(resolve(process.cwd(), level3ProtectPagePath))).toBe(true);
    expect(source).toContain('SystemNetworkSecurityLevel3ProtectConfigIndex');
    expect(source).toContain('ArtEditPage');
    expect(source).toContain('ArtEditSection');
  });

  it('provides a real login-fail management page', () => {
    const source = readFileSync(resolve(process.cwd(), loginFailPagePath), 'utf8');
    expect(existsSync(resolve(process.cwd(), loginFailPagePath))).toBe(true);
    expect(source).toContain('SystemNetworkSecurityLoginFailList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTable');
    expect(source).toContain('batchDeleteLoginFails');
  });

  it('provides a real login-log page, operate-log page, and operate-log drawer', () => {
    expect(readFileSync(resolve(process.cwd(), loginLogPagePath), 'utf8')).toContain(
      'SystemNetworkSecurityLoginLogList',
    );
    expect(readFileSync(resolve(process.cwd(), operateLogPagePath), 'utf8')).toContain(
      'SystemNetworkSecurityOperateLogList',
    );
    expect(readFileSync(resolve(process.cwd(), operateLogDrawerPath), 'utf8')).toContain(
      'SystemNetworkSecurityOperateLogDetailDrawer',
    );
  });

  it('provides capability pages for data masking and api encrypt without module-bridge copy', () => {
    const dataMaskingSource = readFileSync(resolve(process.cwd(), dataMaskingPagePath), 'utf8');
    const apiEncryptSource = readFileSync(resolve(process.cwd(), apiEncryptPagePath), 'utf8');

    expect(dataMaskingSource).toContain('SystemNetworkSecurityDataMaskingList');
    expect(dataMaskingSource).not.toContain('module-bridge');
    expect(apiEncryptSource).toContain('SystemNetworkSecurityApiEncryptIndex');
    expect(apiEncryptSource).toContain('testResponseEncrypt');
    expect(apiEncryptSource).not.toContain('crypto-js');
  });
});
```

- [ ] **Step 2: Write the failing API payload tests**

```ts
// network-protect.test.ts
import { describe, expect, it } from 'vitest';

import {
  buildLevel3ProtectConfigPayload,
  buildLoginFailPageQueryPayload,
  parseLevel3ProtectConfig,
} from './network-protect';

describe('network protect api payloads', () => {
  it('parses config json from the backend config table', () => {
    expect(
      parseLevel3ProtectConfig(
        '{"fileDetectFlag":true,"loginActiveTimeoutMinutes":30,"loginFailLockMinutes":15,"loginFailMaxTimes":3,"maxUploadFileSizeMb":20,"passwordComplexityEnabled":true,"regularChangePasswordMonths":3,"regularChangePasswordNotAllowRepeatTimes":2,"twoFactorLoginEnabled":false}',
      ),
    ).toEqual({
      fileDetectFlag: true,
      loginActiveTimeoutMinutes: 30,
      loginFailLockMinutes: 15,
      loginFailMaxTimes: 3,
      maxUploadFileSizeMb: 20,
      passwordComplexityEnabled: true,
      regularChangePasswordMonths: 3,
      regularChangePasswordNotAllowRepeatTimes: 2,
      twoFactorLoginEnabled: false,
    });
  });

  it('trims login-fail query keywords and preserves paging fields', () => {
    expect(
      buildLoginFailPageQueryPayload({
        lockFlag: true,
        loginLockBeginTimeBegin: '2026-07-01',
        loginLockBeginTimeEnd: '2026-07-05',
        loginName: '  admin  ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      lockFlag: true,
      loginLockBeginTimeBegin: '2026-07-01',
      loginLockBeginTimeEnd: '2026-07-05',
      loginName: 'admin',
      pageNum: 2,
      pageSize: 20,
    });
  });
});
```

```ts
// login-log.test.ts
expect(
  buildLoginLogPageQueryPayload({
    endDate: '2026-07-05',
    ip: ' 127.0.0.1 ',
    pageNum: 1,
    pageSize: 10,
    startDate: '2026-07-01',
    userName: ' admin ',
  }),
).toEqual({
  endDate: '2026-07-05',
  ip: '127.0.0.1',
  pageNum: 1,
  pageSize: 10,
  startDate: '2026-07-01',
  userName: 'admin',
});
```

```ts
// operate-log.test.ts
expect(
  buildOperateLogPageQueryPayload({
    endDate: '2026-07-05',
    keywords: ' 登录 ',
    pageNum: 1,
    pageSize: 10,
    requestKeywords: ' /login ',
    startDate: '2026-07-01',
    successFlag: false,
    userName: ' admin ',
  }),
).toEqual({
  endDate: '2026-07-05',
  keywords: '登录',
  pageNum: 1,
  pageSize: 10,
  requestKeywords: '/login',
  startDate: '2026-07-01',
  successFlag: false,
  userName: 'admin',
});
```

```ts
// data-masking.test.ts
expect(buildDataMaskingDemoPath()).toBe('/support/dataMasking/demo/query');
```

```ts
// api-encrypt.test.ts
expect(
  buildApiEncryptDemoPayload({ age: 18, name: '  Alice  ' }),
).toEqual({ age: 18, name: 'Alice' });
expect(buildApiEncryptEnvelope('cipher-text')).toEqual({
  encryptData: 'cipher-text',
});
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/network-protect.test.ts apps/hunyuan-system/src/api/system/login-log.test.ts apps/hunyuan-system/src/api/system/operate-log.test.ts apps/hunyuan-system/src/api/system/data-masking.test.ts apps/hunyuan-system/src/api/system/api-encrypt.test.ts --dom
```

Expected:

- FAIL because the six page files and five API modules do not exist yet.

- [ ] **Step 4: Commit the failing-contract baseline**

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/login-log.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.test.ts
git commit -m "test: add network security docking contracts"
```

## Task 2: Add Level-3 Protect API Module and Config Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/level3-protect-config-index.vue`

**Interfaces:**
- Consumes:
  - `requestClient` from `#/api/request`
  - `ArtEditPage`, `ArtEditSection` from `@vben/art-hooks/edit`
  - `ArtPageActions` from `@vben/art-hooks/common`
- Produces:
  - `parseLevel3ProtectConfig(raw?: null | string): Level3ProtectConfigFormModel`
  - `buildLevel3ProtectConfigPayload(params: Level3ProtectConfigFormModel): Level3ProtectConfigFormModel`
  - `queryLevel3ProtectConfig(): Promise<Level3ProtectConfigFormModel>`
  - `updateLevel3ProtectConfig(params: Level3ProtectConfigFormModel): Promise<string>`
  - `SystemNetworkSecurityLevel3ProtectConfigIndex`

- [ ] **Step 1: Keep the tests focused on config parsing and mutation**

```ts
// network-protect.test.ts
it('trims and preserves config payload booleans and numbers', () => {
  expect(
    buildLevel3ProtectConfigPayload({
      fileDetectFlag: true,
      loginActiveTimeoutMinutes: 30,
      loginFailLockMinutes: 15,
      loginFailMaxTimes: 3,
      maxUploadFileSizeMb: 20,
      passwordComplexityEnabled: true,
      regularChangePasswordMonths: 3,
      regularChangePasswordNotAllowRepeatTimes: 2,
      twoFactorLoginEnabled: false,
    }),
  ).toEqual({
    fileDetectFlag: true,
    loginActiveTimeoutMinutes: 30,
    loginFailLockMinutes: 15,
    loginFailMaxTimes: 3,
    maxUploadFileSizeMb: 20,
    passwordComplexityEnabled: true,
    regularChangePasswordMonths: 3,
    regularChangePasswordNotAllowRepeatTimes: 2,
    twoFactorLoginEnabled: false,
  });
});
```

- [ ] **Step 2: Implement the API module**

```ts
import { requestClient } from '#/api/request';

export interface Level3ProtectConfigFormModel {
  fileDetectFlag: boolean;
  loginActiveTimeoutMinutes: number;
  loginFailLockMinutes: number;
  loginFailMaxTimes: number;
  maxUploadFileSizeMb: number;
  passwordComplexityEnabled: boolean;
  regularChangePasswordMonths: number;
  regularChangePasswordNotAllowRepeatTimes: number;
  twoFactorLoginEnabled: boolean;
}

export function parseLevel3ProtectConfig(raw?: null | string): Level3ProtectConfigFormModel {
  const parsed = raw ? JSON.parse(raw) : {};
  return {
    fileDetectFlag: Boolean(parsed.fileDetectFlag),
    loginActiveTimeoutMinutes: Number(parsed.loginActiveTimeoutMinutes ?? 30),
    loginFailLockMinutes: Number(parsed.loginFailLockMinutes ?? 30),
    loginFailMaxTimes: Number(parsed.loginFailMaxTimes ?? 3),
    maxUploadFileSizeMb: Number(parsed.maxUploadFileSizeMb ?? 30),
    passwordComplexityEnabled: Boolean(parsed.passwordComplexityEnabled ?? true),
    regularChangePasswordMonths: Number(parsed.regularChangePasswordMonths ?? 3),
    regularChangePasswordNotAllowRepeatTimes: Number(
      parsed.regularChangePasswordNotAllowRepeatTimes ?? 3,
    ),
    twoFactorLoginEnabled: Boolean(parsed.twoFactorLoginEnabled),
  };
}

export function buildLevel3ProtectConfigPayload(
  params: Level3ProtectConfigFormModel,
): Level3ProtectConfigFormModel {
  return { ...params };
}

export async function queryLevel3ProtectConfig() {
  const raw = await requestClient.get<string>('/support/protect/level3protect/getConfig');
  return parseLevel3ProtectConfig(raw);
}

export async function updateLevel3ProtectConfig(params: Level3ProtectConfigFormModel) {
  return requestClient.post<string>(
    '/support/protect/level3protect/updateConfig',
    buildLevel3ProtectConfigPayload(params),
  );
}
```

- [ ] **Step 3: Implement the config page using the shared edit shell**

```vue
<script setup lang="ts">
import type { Level3ProtectConfigFormModel } from '#/api/system/network-protect';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtPageActions } from '@vben/art-hooks/common';
import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';

import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  queryLevel3ProtectConfig,
  updateLevel3ProtectConfig,
} from '#/api/system/network-protect';

defineOptions({ name: 'SystemNetworkSecurityLevel3ProtectConfigIndex' });
// keep one page-level form, one save action, and sectioned config fields
</script>
```

```ts
const rules: FormRules<Level3ProtectConfigFormModel> = {
  loginFailMaxTimes: [{ required: true, message: '请输入连续失败次数', trigger: 'blur' }],
  loginFailLockMinutes: [{ required: true, message: '请输入锁定时长', trigger: 'blur' }],
  loginActiveTimeoutMinutes: [{ required: true, message: '请输入最大在线时长', trigger: 'blur' }],
  regularChangePasswordMonths: [{ required: true, message: '请输入定期改密周期', trigger: 'blur' }],
  regularChangePasswordNotAllowRepeatTimes: [{ required: true, message: '请输入历史密码重复限制', trigger: 'blur' }],
  maxUploadFileSizeMb: [{ required: true, message: '请输入上传大小限制', trigger: 'blur' }],
};
```

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/network-protect.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:

- PASS
- The level-3 protect page now exists at the backend-defined component path.

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.ts hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/level3-protect-config-index.vue
git commit -m "feat: add level3 protect config page"
```

## Task 3: Extend the Protect Module with Login-Fail Management

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/login-fail/login-fail-list.vue`

**Interfaces:**
- Consumes:
  - `PageResult<T>` shape used in existing `api/system/*.ts`
  - `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, `ArtTable`
- Produces:
  - `buildLoginFailPageQueryPayload(params: LoginFailPageQueryParams)`
  - `queryLoginFailPage(params: LoginFailPageQueryParams)`
  - `batchDeleteLoginFails(loginFailIds: number[])`
  - `SystemNetworkSecurityLoginFailList`

- [ ] **Step 1: Add the failing login-fail query-normalization test**

```ts
it('keeps blank login names out of login-fail queries', () => {
  expect(
    buildLoginFailPageQueryPayload({
      lockFlag: undefined,
      loginLockBeginTimeBegin: '',
      loginLockBeginTimeEnd: '',
      loginName: '   ',
      pageNum: 1,
      pageSize: 10,
    }),
  ).toEqual({
    lockFlag: undefined,
    loginLockBeginTimeBegin: undefined,
    loginLockBeginTimeEnd: undefined,
    loginName: undefined,
    pageNum: 1,
    pageSize: 10,
  });
});
```

- [ ] **Step 2: Extend the API module**

```ts
export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface LoginFailRecord {
  createTime?: null | string;
  lockFlag?: null | number;
  loginFailCount: number;
  loginFailId: number;
  loginLockBeginTime?: null | string;
  loginName: string;
  updateTime?: null | string;
  userId?: null | number;
  userType?: null | number;
}

export interface LoginFailPageQueryParams {
  lockFlag?: boolean;
  loginLockBeginTimeBegin?: null | string;
  loginLockBeginTimeEnd?: null | string;
  loginName?: null | string;
  pageNum: number;
  pageSize: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildLoginFailPageQueryPayload(params: LoginFailPageQueryParams) {
  return {
    lockFlag: params.lockFlag,
    loginLockBeginTimeBegin: cleanText(params.loginLockBeginTimeBegin) || undefined,
    loginLockBeginTimeEnd: cleanText(params.loginLockBeginTimeEnd) || undefined,
    loginName: cleanText(params.loginName) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export async function queryLoginFailPage(params: LoginFailPageQueryParams) {
  return requestClient.post<PageResult<LoginFailRecord>>(
    '/support/protect/loginFail/queryPage',
    buildLoginFailPageQueryPayload(params),
  );
}

export async function batchDeleteLoginFails(loginFailIds: number[]) {
  return requestClient.post<string>('/support/protect/loginFail/batchDelete', loginFailIds);
}
```

- [ ] **Step 3: Implement the list page**

```vue
<script setup lang="ts">
import type { LoginFailRecord } from '#/api/system/network-protect';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

defineOptions({ name: 'SystemNetworkSecurityLoginFailList' });
// keep dense list-page structure; use selection + batch clear
</script>
```

```ts
const columnsFactory = (): ColumnOption<LoginFailRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'loginName', label: '登录名', minWidth: 180 },
  { prop: 'loginFailCount', label: '失败次数', width: 100, align: 'center' },
  { prop: 'lockFlag', label: '锁定状态', width: 100, align: 'center', useSlot: true },
  { prop: 'loginLockBeginTime', label: '锁定开始时间', minWidth: 180 },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
];
```

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/network-protect.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.ts hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/login-fail/login-fail-list.vue
git commit -m "feat: add login fail management page"
```

## Task 4: Add the Login-Log Audit Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/login-log.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/login-log.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/login-log/login-log-list.vue`

**Interfaces:**
- Consumes:
  - Search and table patterns from `apps/hunyuan-system/src/views/support/sms/send-log-list.vue`
- Produces:
  - `buildLoginLogPageQueryPayload(params: LoginLogPageQueryParams)`
  - `queryLoginLogPage(params: LoginLogPageQueryParams)`
  - `SystemNetworkSecurityLoginLogList`

- [ ] **Step 1: Add the payload test**

```ts
import { describe, expect, it } from 'vitest';

import { buildLoginLogPageQueryPayload } from './login-log';

describe('login log api payloads', () => {
  it('trims login log filters and preserves dates', () => {
    expect(
      buildLoginLogPageQueryPayload({
        endDate: '2026-07-05',
        ip: ' 10.0.0.8 ',
        pageNum: 2,
        pageSize: 20,
        startDate: '2026-07-01',
        userName: ' admin ',
      }),
    ).toEqual({
      endDate: '2026-07-05',
      ip: '10.0.0.8',
      pageNum: 2,
      pageSize: 20,
      startDate: '2026-07-01',
      userName: 'admin',
    });
  });
});
```

- [ ] **Step 2: Implement the API module**

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

export interface LoginLogRecord {
  createTime?: null | string;
  loginDevice?: null | string;
  loginIp?: null | string;
  loginIpRegion?: null | string;
  loginLogId: number;
  loginResult?: null | number;
  remark?: null | string;
  userAgent?: null | string;
  userId?: null | number;
  userName?: null | string;
  userType?: null | number;
}

export interface LoginLogPageQueryParams {
  endDate?: null | string;
  ip?: null | string;
  pageNum: number;
  pageSize: number;
  startDate?: null | string;
  userName?: null | string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}
```

```ts
export function buildLoginLogPageQueryPayload(params: LoginLogPageQueryParams) {
  return {
    endDate: cleanText(params.endDate) || undefined,
    ip: cleanText(params.ip) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    startDate: cleanText(params.startDate) || undefined,
    userName: cleanText(params.userName) || undefined,
  };
}

export async function queryLoginLogPage(params: LoginLogPageQueryParams) {
  return requestClient.post<PageResult<LoginLogRecord>>(
    '/support/loginLog/page/query',
    buildLoginLogPageQueryPayload(params),
  );
}
```

- [ ] **Step 3: Implement the page**

```vue
defineOptions({ name: 'SystemNetworkSecurityLoginLogList' });

const resultLabelMap = {
  1: '登录成功',
  2: '登录失败',
  3: '退出登录',
} as const;
```

```ts
const columnsFactory = (): ColumnOption<LoginLogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'userName', label: '用户名称', minWidth: 160 },
  { prop: 'loginIp', label: '登录 IP', minWidth: 140 },
  { prop: 'loginIpRegion', label: 'IP 地区', minWidth: 160 },
  { prop: 'loginDevice', label: '登录设备', minWidth: 120 },
  { prop: 'loginResult', label: '结果', width: 100, align: 'center', useSlot: true },
  { prop: 'remark', label: '备注', minWidth: 200, formatter: (row) => row.remark || '-' },
  { prop: 'createTime', label: '记录时间', minWidth: 180 },
];
```

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/login-log.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/login-log.ts hunyuan-design/apps/hunyuan-system/src/api/system/login-log.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/login-log/login-log-list.vue
git commit -m "feat: add login log audit page"
```

## Task 5: Add Operate-Log Query, Detail Drawer, and Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/components/operate-log-detail-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/operate-log-list.vue`

**Interfaces:**
- Consumes:
  - `useVbenDrawer` pattern from `apps/hunyuan-system/src/views/support/dict/index.vue`
  - `ArtDetail` pattern from `apps/web-ele/src/views/demos/detail-test.vue`
- Produces:
  - `buildOperateLogPageQueryPayload(params: OperateLogPageQueryParams)`
  - `queryOperateLogPage(params: OperateLogPageQueryParams)`
  - `buildOperateLogDetailPath(operateLogId: number)`
  - `queryOperateLogDetail(operateLogId: number)`
  - `SystemNetworkSecurityOperateLogDetailDrawer`
  - `SystemNetworkSecurityOperateLogList`

- [ ] **Step 1: Write the drawer and detail-path tests**

```ts
it('builds the operate-log detail path from the row id', () => {
  expect(buildOperateLogDetailPath(12)).toBe('/support/operateLog/detail/12');
});
```

```ts
// network-security-modules.test.ts
expect(readFileSync(resolve(process.cwd(), operateLogPagePath), 'utf8')).toContain(
  'openOperateLogDetail',
);
expect(readFileSync(resolve(process.cwd(), operateLogDrawerPath), 'utf8')).toContain(
  'ArtDetail',
);
```

- [ ] **Step 2: Implement the API module**

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

export interface OperateLogRecord {
  content?: null | string;
  createTime?: null | string;
  failReason?: null | string;
  ip?: null | string;
  ipRegion?: null | string;
  method?: null | string;
  module?: null | string;
  operateLogId: number;
  operateUserName?: null | string;
  param?: null | string;
  response?: null | string;
  successFlag?: null | boolean;
  updateTime?: null | string;
  url?: null | string;
  userAgent?: null | string;
}

export interface OperateLogPageQueryParams {
  endDate?: null | string;
  keywords?: null | string;
  pageNum: number;
  pageSize: number;
  requestKeywords?: null | string;
  startDate?: null | string;
  successFlag?: null | boolean;
  userName?: null | string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildOperateLogPageQueryPayload(params: OperateLogPageQueryParams) {
  return {
    endDate: cleanText(params.endDate) || undefined,
    keywords: cleanText(params.keywords) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    requestKeywords: cleanText(params.requestKeywords) || undefined,
    startDate: cleanText(params.startDate) || undefined,
    successFlag: params.successFlag,
    userName: cleanText(params.userName) || undefined,
  };
}

export async function queryOperateLogPage(params: OperateLogPageQueryParams) {
  return requestClient.post<PageResult<OperateLogRecord>>(
    '/support/operateLog/queryPage',
    buildOperateLogPageQueryPayload(params),
  );
}

export function buildOperateLogDetailPath(operateLogId: number) {
  return `/support/operateLog/detail/${operateLogId}`;
}

export async function queryOperateLogDetail(operateLogId: number) {
  return requestClient.get<OperateLogRecord>(buildOperateLogDetailPath(operateLogId));
}
```

- [ ] **Step 3: Implement the detail drawer**

```vue
<script setup lang="ts">
import type { OperateLogRecord } from '#/api/system/operate-log';
import type { DetailSection } from '@vben/art-hooks/detail';

import { computed, ref, watch } from 'vue';

import { ArtDetail } from '@vben/art-hooks/detail';

import { ElSkeleton } from 'element-plus';

import { queryOperateLogDetail } from '#/api/system/operate-log';

defineOptions({ name: 'SystemNetworkSecurityOperateLogDetailDrawer' });
// load the detail lazily when a row is provided
</script>
```

```ts
const sections = computed<DetailSection<OperateLogRecord>[]>(() => [
  {
    key: 'basic',
    title: '基本信息',
    items: [
      { label: '操作人', prop: 'operateUserName' },
      { label: '模块', prop: 'module' },
      { label: '请求方法', prop: 'method' },
      { label: '请求地址', prop: 'url', span: 2 },
      { label: '请求结果', prop: 'successFlag', useSlot: true },
      { label: '失败原因', prop: 'failReason', span: 2 },
      { label: '请求参数', prop: 'param', span: 3 },
      { label: '返回结果', prop: 'response', span: 3 },
    ],
  },
]);
```

- [ ] **Step 4: Implement the list page with drawer entry**

```vue
defineOptions({ name: 'SystemNetworkSecurityOperateLogList' });

const [OperateLogDetailDrawer, operateLogDetailDrawerApi] = useVbenDrawer({
  connectedComponent: OperateLogDetailDrawerPanel,
  destroyOnClose: false,
});

function openOperateLogDetail(row: OperateLogRecord) {
  currentDetailRow.value = row;
  operateLogDetailDrawerApi.open();
}
```

- [ ] **Step 5: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/operate-log.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.ts hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/components/operate-log-detail-drawer.vue hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/operate-log-list.vue
git commit -m "feat: add operate log page and detail drawer"
```

## Task 6: Add the Data-Masking Capability Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue`

**Interfaces:**
- Consumes:
  - Backend demo endpoint `/support/dataMasking/demo/query`
  - Existing quiet list-page structure
- Produces:
  - `buildDataMaskingDemoPath(): string`
  - `queryDataMaskingDemoList(): Promise<DataMaskingDemoRecord[]>`
  - `SystemNetworkSecurityDataMaskingList`

- [ ] **Step 1: Keep the API test minimal and explicit**

```ts
import { describe, expect, it } from 'vitest';

import { buildDataMaskingDemoPath } from './data-masking';

describe('data masking api paths', () => {
  it('targets the backend demo endpoint under the support prefix', () => {
    expect(buildDataMaskingDemoPath()).toBe('/support/dataMasking/demo/query');
  });
});
```

- [ ] **Step 2: Implement the API module**

```ts
import { requestClient } from '#/api/request';

export interface DataMaskingDemoRecord {
  address?: null | string;
  bankCard?: null | string;
  carLicense?: null | string;
  email?: null | string;
  idCard?: null | string;
  other?: null | string;
  password?: null | string;
  phone?: null | string;
  userId?: null | number;
}

export function buildDataMaskingDemoPath() {
  return '/support/dataMasking/demo/query';
}

export async function queryDataMaskingDemoList() {
  return requestClient.get<DataMaskingDemoRecord[]>(buildDataMaskingDemoPath());
}
```

- [ ] **Step 3: Implement the capability page as a quiet demo list**

```vue
defineOptions({ name: 'SystemNetworkSecurityDataMaskingList' });

const columnsFactory = (): ColumnOption<DataMaskingDemoRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'userId', label: '用户 ID', minWidth: 140 },
  { prop: 'phone', label: '手机号', minWidth: 140 },
  { prop: 'idCard', label: '身份证号', minWidth: 180 },
  { prop: 'address', label: '地址', minWidth: 220 },
  { prop: 'email', label: '邮箱', minWidth: 180 },
  { prop: 'bankCard', label: '银行卡', minWidth: 200 },
  { prop: 'other', label: '其他字段', minWidth: 160 },
];
```

```vue
<ArtTableHeader
  v-model="columnChecks"
  :loading="loading"
  layout="fullscreen,columns,settings"
>
  <template #left>
    <ElButton type="primary" @click="loadData">刷新演示数据</ElButton>
  </template>
</ArtTableHeader>
```

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/data-masking.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.ts hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue
git commit -m "feat: add data masking capability page"
```

## Task 7: Add the First-Phase API-Encrypt Capability Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/api-encrypt/api-encrypt-index.vue`

**Interfaces:**
- Consumes:
  - Backend support endpoints:
    - `/support/apiEncrypt/testResponseEncrypt`
    - `/support/apiEncrypt/testRequestEncrypt`
    - `/support/apiEncrypt/testDecryptAndEncrypt`
    - `/support/apiEncrypt/testArray`
- Produces:
  - `buildApiEncryptDemoPayload(params: ApiEncryptDemoPayload)`
  - `buildApiEncryptEnvelope(encryptData: string)`
  - `testResponseEncryptDemo(params: ApiEncryptDemoPayload)`
  - `SystemNetworkSecurityApiEncryptIndex`

- [ ] **Step 1: Make the tests reflect the phase-one scope**

```ts
import { describe, expect, it } from 'vitest';

import {
  buildApiEncryptDemoPayload,
  buildApiEncryptEnvelope,
} from './api-encrypt';

describe('api encrypt capability helpers', () => {
  it('trims the live response-encrypt demo payload', () => {
    expect(
      buildApiEncryptDemoPayload({
        age: 18,
        name: '  Alice  ',
      }),
    ).toEqual({
      age: 18,
      name: 'Alice',
    });
  });

  it('builds encrypted request envelopes for static contract examples', () => {
    expect(buildApiEncryptEnvelope('cipher-text')).toEqual({
      encryptData: 'cipher-text',
    });
  });
});
```

- [ ] **Step 2: Implement the API module without introducing a new crypto dependency**

```ts
import { requestClient } from '#/api/request';

export interface ApiEncryptDemoPayload {
  age: number;
  name: string;
}

export interface ApiEncryptEnvelope {
  encryptData: string;
}

export function buildApiEncryptDemoPayload(params: ApiEncryptDemoPayload) {
  return {
    age: params.age,
    name: params.name.trim(),
  };
}

export function buildApiEncryptEnvelope(encryptData: string): ApiEncryptEnvelope {
  return { encryptData };
}

export async function testResponseEncryptDemo(params: ApiEncryptDemoPayload) {
  return requestClient.post<string>(
    '/support/apiEncrypt/testResponseEncrypt',
    buildApiEncryptDemoPayload(params),
  );
}
```

- [ ] **Step 3: Implement the capability page with one live demo and explicit deferred sections**

```vue
<script setup lang="ts">
import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElTag,
} from 'element-plus';

import {
  buildApiEncryptEnvelope,
  testResponseEncryptDemo,
} from '#/api/system/api-encrypt';

defineOptions({ name: 'SystemNetworkSecurityApiEncryptIndex' });
// one live panel for response encryption, three explanatory panels for encrypted-request contracts
</script>
```

```ts
const sampleEnvelope = computed(() =>
  JSON.stringify(buildApiEncryptEnvelope('base64-cipher-text'), null, 2),
);
```

```vue
<ElCard>
  <template #header>返回加密 Live Demo</template>
  <!-- name + age form -->
</ElCard>

<ElCard>
  <template #header>请求加密 Contract</template>
  <ElTag type="warning">第一阶段不新增前端加密实现</ElTag>
  <pre>{{ sampleEnvelope }}</pre>
</ElCard>
```

- [ ] **Step 4: Run targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/api-encrypt.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.ts hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/api-encrypt/api-encrypt-index.vue
git commit -m "feat: add api encrypt capability page"
```

## Task 8: Final Network-Security Closure Verification

**Files:**
- Modify only if direct verification reveals a scoped issue:
  - `hunyuan-design/apps/hunyuan-system/src/api/system/*.ts`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/**/*.vue`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts`

**Interfaces:**
- Consumes:
  - The six new page files at the exact backend-defined component paths
  - The five new API modules
- Produces:
  - Verified replacement of the network-security bridge pages without route-infrastructure changes

- [ ] **Step 1: Run all network-security unit and source-contract tests**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/network-protect.test.ts apps/hunyuan-system/src/api/system/login-log.test.ts apps/hunyuan-system/src/api/system/operate-log.test.ts apps/hunyuan-system/src/api/system/data-masking.test.ts apps/hunyuan-system/src/api/system/api-encrypt.test.ts apps/hunyuan-system/src/views/support/network-security-modules.test.ts --dom
```

Expected: PASS

- [ ] **Step 2: Run frontend typechecks**

Run:

```bash
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS

- [ ] **Step 3: Verify that the backend component paths now resolve to local pages**

Run:

```bash
rg -n "level3-protect-config-index|login-fail-list|login-log-list|operate-log-list|data-masking-list|api-encrypt-index" hunyuan-design/apps/hunyuan-system/src/views
rg -n "MODULE_BRIDGE_COMPONENT|normalizeComponentPath" hunyuan-design/apps/hunyuan-system/src/api/core/login-adapter.ts
```

Expected:

- The six real page files exist under `apps/hunyuan-system/src/views/support/...`
- `login-adapter.ts` remains unchanged
- No network-security task required a route-layer patch

- [ ] **Step 4: Commit final verification fixes if needed**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.ts hunyuan-design/apps/hunyuan-system/src/api/system/network-protect.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/login-log.ts hunyuan-design/apps/hunyuan-system/src/api/system/login-log.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.ts hunyuan-design/apps/hunyuan-system/src/api/system/operate-log.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.ts hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.ts hunyuan-design/apps/hunyuan-system/src/api/system/api-encrypt.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/network-security-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/level3-protect-config-index.vue hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue hunyuan-design/apps/hunyuan-system/src/views/support/login-fail/login-fail-list.vue hunyuan-design/apps/hunyuan-system/src/views/support/login-log/login-log-list.vue hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/components/operate-log-detail-drawer.vue hunyuan-design/apps/hunyuan-system/src/views/support/operate-log/operate-log-list.vue hunyuan-design/apps/hunyuan-system/src/views/support/api-encrypt/api-encrypt-index.vue
git commit -m "test: verify network security docking"
```

## Self-Review

### Spec coverage

- The four management pages from the spec each have a dedicated implementation task:
  - `三级等保设置` -> Task 2
  - `登录失败锁定` -> Task 3
  - `登录登出记录` -> Task 4
  - `用户操作记录` -> Task 5
- The two capability pages each have a dedicated implementation task:
  - `敏感数据脱敏` -> Task 6
  - `接口加解密` -> Task 7
- The plan keeps menu loading and route fallback unchanged, exactly as required by the spec.
- Intentional scope refinement:
  - The spec described `接口加解密` as a broader capability page.
  - This plan narrows phase-one live execution to `testResponseEncrypt`.
  - Reason: the repo does not currently expose an explicit frontend crypto utility or explicit crypto dependency approval, so live request-encrypt demos would silently expand the task into frontend cryptography work.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task includes exact file paths, core code snippets, commands, and expected outcomes.

### Type consistency

- `network-protect.ts` owns both config and login-fail types; later pages reuse those exact names.
- `operate-log.ts` defines both list-row and detail accessors used by the drawer.
- `api-encrypt.ts` exposes only the helpers the page actually needs in phase one.
