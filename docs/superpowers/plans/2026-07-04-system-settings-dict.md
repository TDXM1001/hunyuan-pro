# System Settings Dictionary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the system-settings `module-bridge` entry for dictionary management with a real backend-wired page at `views/support/dict/index.vue`, including dictionary main-table and dictionary-item child-table closure.

**Architecture:** Keep the existing backend-menu loading, dynamic-route mapping, and `module-bridge` fallback unchanged. Add one focused `dict.ts` API module, one API payload test file, extend the support-module source contract test, and land one two-pane dictionary page that follows the existing dense management-page patterns while reusing the employee page's split-pane idea.

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc

## Global Constraints

- Follow `AGENTS.md`: make one incremental change at a time.
- Follow `AGENTS.md`: explain why a change is needed before editing files.
- Follow `AGENTS.md`: prefer existing project patterns over new abstractions.
- Follow `AGENTS.md`: do not add new dependencies without explicit approval.
- Follow `docs/frontend-list-table-page-standard.md` for list/search/table pages.
- Strictly use the backend menu component path `/support/dict/index.vue`; do not invent a different route or page path.
- Keep login, backend menu loading, `login-adapter.ts`, and `module-bridge` behavior unchanged.
- Keep request logic in `apps/hunyuan-system/src/api/system/*.ts`, not in shared `@vben/art-hooks` components.
- Implement only the dictionary increment in this plan.
- Use UTF-8 for reads and writes.
- Use concise Chinese code comments only where logic is not self-explanatory.
- Verify meaningful frontend changes with `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`.

---

## File Structure

### Test Contracts

- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
  - Extend support-module source contracts from parameter-config to dictionary management.

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/dict.test.ts`
  - Lock query payload trimming, dictionary mutation payload normalization, and dictionary-item payload normalization.

### API Module

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/dict.ts`
  - Define `PageResult`, `DictRecord`, `DictDataRecord`, query DTOs, form DTOs, payload builders, and request functions for both dictionary and dictionary-item endpoints.

### Page Module

- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/dict/index.vue`
  - Render a dense split-pane page.
  - Left pane: dictionary search + table + add/edit dialog.
  - Right pane: selected dictionary's data-item table + add/edit dialog.
  - Support toggle enable/disable and delete flows for both levels.

## Task 1: Lock the Dictionary Module Contract with Source and API Tests

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/dict.test.ts`

**Interfaces:**
- Consumes:
  - `existsSync`, `readFileSync`, `resolve` from Node fs/path
  - `describe`, `expect`, `it` from Vitest
- Produces:
  - Source-level checks that the dictionary page exists at the backend-defined path.
  - API-level checks that query, dictionary mutation, and dictionary-item mutation payloads trim text correctly.

- [ ] **Step 1: Extend the failing support-module source contract**

Modify `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts` by appending:

```ts
const dictPagePath = 'apps/hunyuan-system/src/views/support/dict/index.vue';
const dictApiPath = 'apps/hunyuan-system/src/api/system/dict.ts';
```

and add these tests inside `describe('system settings support modules', ...)`:

```ts
  it('provides a real dictionary management page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), dictPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportDictIndex');
  });

  it('keeps the dictionary page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), dictPagePath), 'utf8');

    expect(source).not.toContain('dict-page__title');
    expect(source).not.toContain('dict-page__hero');
    expect(source).not.toContain('dict-page__desc');
    expect(source).toContain(':collapsible="false"');
    expect(source).toContain('grid-template-columns: 320px minmax(0, 1fr);');
  });

  it('wires the dictionary api module to the backend dict and dictData endpoints', () => {
    const apiPath = resolve(process.cwd(), dictApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/dict/queryPage'");
    expect(source).toContain("'/dict/add'");
    expect(source).toContain("'/dict/update'");
    expect(source).toContain("'/dict/dictData/queryDictData/'");
    expect(source).toContain("'/dict/dictData/add'");
    expect(source).toContain("'/dict/dictData/update'");
    expect(source).toContain('buildDictPageQueryPayload');
    expect(source).toContain('buildDictMutationPayload');
    expect(source).toContain('buildDictDataMutationPayload');
  });

  it('surfaces dictionary and dictionary-item key fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), dictPagePath), 'utf8');

    expect(source).toContain('dictName');
    expect(source).toContain('dictCode');
    expect(source).toContain('dataLabel');
    expect(source).toContain('dataValue');
    expect(source).toContain('dataStyle');
    expect(source).toContain('disabledFlag');
  });
```

- [ ] **Step 2: Create the failing API payload test**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/dict.test.ts` with:

```ts
import { describe, expect, it } from 'vitest';

import {
  buildDictDataMutationPayload,
  buildDictMutationPayload,
  buildDictPageQueryPayload,
} from './dict';

describe('dictionary api payloads', () => {
  it('trims dictionary page query keywords and preserves paging fields', () => {
    expect(
      buildDictPageQueryPayload({
        disabledFlag: false,
        keywords: '  status  ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      disabledFlag: false,
      keywords: 'status',
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('omits blank dictionary keywords after trimming', () => {
    expect(
      buildDictPageQueryPayload({
        disabledFlag: undefined,
        keywords: '   ',
        pageNum: 1,
        pageSize: 10,
      }),
    ).toEqual({
      disabledFlag: undefined,
      keywords: undefined,
      pageNum: 1,
      pageSize: 10,
    });
  });

  it('trims dictionary mutation payload fields', () => {
    expect(
      buildDictMutationPayload({
        dictCode: '  sys_status  ',
        dictId: 7,
        dictName: '  状态字典  ',
        remark: '  系统状态项  ',
      }),
    ).toEqual({
      dictCode: 'sys_status',
      dictId: 7,
      dictName: '状态字典',
      remark: '系统状态项',
    });
  });

  it('trims dictionary-item mutation payload fields and preserves ids', () => {
    expect(
      buildDictDataMutationPayload({
        dataLabel: '  启用  ',
        dataStyle: '  success  ',
        dataValue: '  Y  ',
        dictCode: 'sys_status',
        dictDataId: 11,
        dictId: 7,
        remark: '  默认开启  ',
        sortOrder: 99,
      }),
    ).toEqual({
      dataLabel: '启用',
      dataStyle: 'success',
      dataValue: 'Y',
      dictCode: 'sys_status',
      dictDataId: 11,
      dictId: 7,
      remark: '默认开启',
      sortOrder: 99,
    });
  });
});
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/dict.test.ts --dom
```

Expected:

- The support-module source contract fails because `views/support/dict/index.vue` and `api/system/dict.ts` do not exist yet.
- The API payload test fails because `./dict` does not exist yet.

## Task 2: Add the Dictionary API Module

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/dict.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/api/system/dict.test.ts`

**Interfaces:**
- Consumes: `requestClient` from `#/api/request`
- Produces:
  - `PageResult<T>`
  - `DictRecord`
  - `DictDataRecord`
  - `DictPageQueryParams`
  - `DictAddForm`
  - `DictUpdateForm`
  - `DictDataAddForm`
  - `DictDataUpdateForm`
  - `buildDictPageQueryPayload(params: DictPageQueryParams)`
  - `buildDictMutationPayload<T extends DictAddForm | DictUpdateForm>(params: T)`
  - `buildDictDataMutationPayload<T extends DictDataAddForm | DictDataUpdateForm>(params: T)`
  - `queryDictPage(params: DictPageQueryParams)`
  - `addDict(params: DictAddForm)`
  - `updateDict(params: DictUpdateForm)`
  - `toggleDictDisabled(dictId: number)`
  - `batchDeleteDicts(dictIds: number[])`
  - `deleteDict(dictId: number)`
  - `queryDictDataList(dictId: number)`
  - `addDictData(params: DictDataAddForm)`
  - `updateDictData(params: DictDataUpdateForm)`
  - `toggleDictDataDisabled(dictDataId: number)`
  - `batchDeleteDictData(dictDataIds: number[])`
  - `deleteDictData(dictDataId: number)`

- [ ] **Step 1: Create the API file**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/dict.ts` with the typed payload builders and request functions described above. The payload rules are:

```ts
function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildDictPageQueryPayload(params: DictPageQueryParams) {
  const keywords = cleanText(params.keywords);

  return {
    disabledFlag: params.disabledFlag,
    keywords: keywords || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export function buildDictMutationPayload<T extends DictAddForm | DictUpdateForm>(
  params: T,
): T {
  return {
    ...params,
    dictCode: params.dictCode.trim(),
    dictName: params.dictName.trim(),
    remark: cleanText(params.remark),
  };
}

export function buildDictDataMutationPayload<
  T extends DictDataAddForm | DictDataUpdateForm,
>(params: T): T {
  return {
    ...params,
    dataLabel: params.dataLabel.trim(),
    dataStyle: cleanText(params.dataStyle) || undefined,
    dataValue: params.dataValue.trim(),
    remark: cleanText(params.remark),
  };
}
```

- [ ] **Step 2: Run the API payload test**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/dict.test.ts --dom
```

Expected: PASS.

- [ ] **Step 3: Run typecheck**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

## Task 3: Add the Real Dictionary Management Page

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/views/support/dict/index.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

**Interfaces:**
- Consumes:
  - `DictAddForm`, `DictDataAddForm`, `DictDataRecord`, `DictDataUpdateForm`, `DictPageQueryParams`, `DictRecord`, `DictUpdateForm`
  - `addDict`, `addDictData`, `batchDeleteDictData`, `batchDeleteDicts`, `deleteDict`, `deleteDictData`, `queryDictDataList`, `queryDictPage`, `toggleDictDataDisabled`, `toggleDictDisabled`, `updateDict`, `updateDictData`
- Produces:
  - Vue component name `SystemSupportDictIndex`
  - Real page file at backend-defined component path `/support/dict/index.vue`
  - Left dictionary table and right dictionary-item table with dialog forms

- [ ] **Step 1: Create the Vue page**

Create `hunyuan-design/apps/hunyuan-system/src/views/support/dict/index.vue` with these structural requirements:

```ts
defineOptions({ name: 'SystemSupportDictIndex' });

const dictSearchKeyword = ref('');
const dictSearchDisabledFlag = ref<boolean>();
const dictRows = ref<DictRecord[]>([]);
const activeDictId = ref<null | number>(null);
const activeDict = ref<DictRecord>();
const dictDataRows = ref<DictDataRecord[]>([]);
const selectedDictRows = ref<DictRecord[]>([]);
const selectedDictDataRows = ref<DictDataRecord[]>([]);
```

The page layout must be:

```vue
<Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
  <div class="dict-page">
    <section class="dict-page__types-column">
      <!-- search card -->
      <!-- dictionary table card -->
    </section>

    <section class="dict-page__data-column">
      <!-- dictionary-item table card -->
    </section>
  </div>

  <!-- dictionary dialog -->
  <!-- dictionary-item dialog -->
</Page>
```

The left-column search must use:

```vue
<ArtSearchPanel
  :collapsible="false"
  :loading="dictLoading"
  reset-text="重置"
  search-text="查询"
  :show-refresh="false"
  @reset="handleDictReset"
  @search="handleDictSearch"
>
  <ElFormItem class="dict-page__keyword-item" label="关键字">
    <ElInput
      v-model="dictSearchKeyword"
      clearable
      placeholder="请输入字典名称或编码"
      @keyup.enter="handleDictSearch"
    />
  </ElFormItem>
  <ElFormItem class="dict-page__status-item" label="状态">
    <ElSelect
      v-model="dictSearchDisabledFlag"
      clearable
      placeholder="请选择状态"
    >
      <ElOption :value="false" label="启用" />
      <ElOption :value="true" label="禁用" />
    </ElSelect>
  </ElFormItem>
</ArtSearchPanel>
```

The right-column item table must render only when a dictionary is selected; otherwise show a quiet empty state:

```vue
<ElEmpty description="请选择左侧字典" :image-size="96" />
```

The dictionary-item style options must come from fixed local options matching backend enum values:

```ts
const dictDataStyleOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'danger' },
];
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

## Task 4: Verify Route Matching and Increment Closure

**Files:**
- Modify only if targeted verification reveals a direct issue:
  - `hunyuan-design/apps/hunyuan-system/src/api/system/dict.ts`
  - `hunyuan-design/apps/hunyuan-system/src/api/system/dict.test.ts`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/dict/index.vue`
  - `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

**Interfaces:**
- Consumes:
  - Existing backend-menu loading via `apps/hunyuan-system/src/api/core/auth.ts`
  - Existing dynamic-route adapter via `apps/hunyuan-system/src/api/core/login-adapter.ts`
  - Backend menu component path `/support/dict/index.vue`
- Produces:
  - Verified second system-settings increment with no route-framework changes

- [ ] **Step 1: Run all targeted checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/dict.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all PASS.

- [ ] **Step 2: Verify source-level route matching assumptions**

Run:

```bash
rg -n "normalizeComponentPath|module-bridge|/support/dict/index.vue|SystemSupportDictIndex" hunyuan-design/apps/hunyuan-system/src
```

Expected output includes:

- `apps/hunyuan-system/src/api/core/login-adapter.ts` normalization logic
- `apps/hunyuan-system/src/views/system/module-bridge/index.vue`
- `apps/hunyuan-system/src/views/support/dict/index.vue`
- `defineOptions({ name: 'SystemSupportDictIndex' })`

- [ ] **Step 3: Check final working tree scope**

Run:

```bash
git status --short
```

Expected:

- Intended tracked changes only for parameter-config and dictionary increments
- Pre-existing unrelated untracked files may still remain, such as:

```text
.playwright-mcp/
.superpowers/
hunyuan-system-home-snapshot.md
lefthook.yml
```

## Self-Review

### Spec coverage

- Backend-defined component path `/support/dict/index.vue` is implemented directly in Task 3.
- Dictionary main-table and dictionary-item API closure is implemented in Task 2 and consumed in Task 3.
- Dense split-pane page standard is implemented in Task 3 and checked in Task 1.
- Dynamic route closure is verified in Task 4 without changing login/menu framework code.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every verification step contains exact commands and expected outcomes.

### Type consistency

- `DictRecord`, `DictDataRecord`, `DictPageQueryParams`, `DictAddForm`, `DictUpdateForm`, `DictDataAddForm`, and `DictDataUpdateForm` are defined in Task 2 and consumed consistently in Task 3.
- `buildDictPageQueryPayload`, `buildDictMutationPayload`, and `buildDictDataMutationPayload` are defined in Task 2 and asserted in Tasks 1 and 2.
- The page component name is consistently `SystemSupportDictIndex`.
