# BPM P3.1c Employee Select Usability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `EMPLOYEE_SELECT_AT_START` usable from the Hunyuan BPM designer and runtime start form by adding a form-schema-backed field selector and a single-employee runtime form field.

**Architecture:** Keep the backend P3.1b assignment resolver unchanged. Add frontend-only adapter helpers: one extracts candidate employee fields from `formSchemaJson`, and one normalizes runtime form-create rules so `employeeSelect` fields render as single-select employee pickers. Wire the existing designer detail `formSchemaJson` through `model-editor.vue` into `BpmProcessDesignerAdapter`.

**Tech Stack:** Vue 3, TypeScript, Element Plus, form-create, Vitest, Java 17, Maven.

## Global Constraints

- All production code stays in `E:/my-project/hunyuan-pro`.
- Yudao/RuoYi are reference lines only; do not migrate their enums, APIs, routes, page shells, or module boundaries.
- Do not add dependencies.
- Do not add SQL.
- Do not change backend `SimpleModelValidator` signatures in P3.1c.
- Do not expand to multi-employee approval, countersign, role groups, posts, expressions, or multi-instance.
- Keep Flowable hidden behind Hunyuan BPM boundaries.
- Keep Chinese UI copy, docs, and comments UTF-8.

---

## File Structure

- Create `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/employee-select-field-options.ts`
  - Parses `formSchemaJson` and returns selectable employee field options.
- Modify `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
  - Adds source-level contract tests for employee field option extraction.
- Modify `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
  - Accepts `formSchemaJson`, shows field options for `EMPLOYEE_SELECT_AT_START`, and validates selected field key.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
  - Stores `detail.formSchemaJson` and passes it to the process designer adapter.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
  - Locks the model editor and runtime renderer source contracts.
- Create `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.ts`
  - Normalizes runtime form-create rules for `employeeSelect` fields.
- Create `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts`
  - Tests employee-select runtime rule normalization.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue`
  - Uses the normalization helper and `queryEmployeePage` to provide single-employee options.
- Create `docs/superpowers/specs/2026-07-10-bpm-p3-employee-select-usability-acceptance.md`
  - Records actual verification results and remaining boundaries.

---

### Task 1: Form Schema Employee Field Options And Process Designer Selector

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/employee-select-field-options.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`

**Interfaces:**
- Consumes: `formSchemaJson?: string`, `BpmProcessNodeDraft.employeeSelectFieldKey`.
- Produces:
  - `export interface BpmEmployeeSelectFieldOption { field: string; label: string; }`
  - `export function extractEmployeeSelectFieldOptions(schemaJson?: string): BpmEmployeeSelectFieldOption[]`
  - `BpmProcessDesignerAdapter` prop `formSchemaJson?: string`.

- [ ] **Step 1: Write failing adapter tests**

Add this import to `bpm-designer-adapters.test.ts`:

```ts
import { extractEmployeeSelectFieldOptions } from './employee-select-field-options';
```

Add these tests before the BPMN XML preview test:

```ts
  it('从表单 schema 提取员工单选字段候选项', () => {
    const options = extractEmployeeSelectFieldOptions(
      JSON.stringify({
        fields: [
          {
            field: 'approverEmployeeId',
            title: '审批人',
            type: 'employeeSelect',
          },
          {
            field: 'amount',
            title: '金额',
            type: 'input',
          },
        ],
      }),
    );

    expect(options).toEqual([
      {
        field: 'approverEmployeeId',
        label: '审批人',
      },
    ]);
  });

  it('兼容字段名带员工语义但组件类型尚未标准化的表单字段', () => {
    const options = extractEmployeeSelectFieldOptions(
      JSON.stringify([
        {
          field: 'backupApproverEmployeeId',
          title: '备选审批人',
          type: 'input',
        },
      ]),
    );

    expect(options).toEqual([
      {
        field: 'backupApproverEmployeeId',
        label: '备选审批人',
      },
    ]);
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

Expected: FAIL because `employee-select-field-options.ts` does not exist.

- [ ] **Step 3: Add employee field option extractor**

Create `employee-select-field-options.ts`:

```ts
export interface BpmEmployeeSelectFieldOption {
  field: string;
  label: string;
}

type SchemaNode = Record<string, any>;

function safeParseSchema(schemaJson?: string): unknown {
  if (!schemaJson?.trim()) {
    return [];
  }

  try {
    return JSON.parse(schemaJson);
  } catch {
    return [];
  }
}

function collectSchemaNodes(value: unknown): SchemaNode[] {
  if (Array.isArray(value)) {
    return value.flatMap((item) => collectSchemaNodes(item));
  }
  if (!value || typeof value !== 'object') {
    return [];
  }

  const node = value as SchemaNode;
  const children = [
    ...collectSchemaNodes(node.children),
    ...collectSchemaNodes(node.fields),
  ];

  return [node, ...children];
}

function hasEmployeeSelectType(node: SchemaNode) {
  const values = [
    node.type,
    node.component,
    node.props?.type,
    node.props?.component,
  ]
    .filter(Boolean)
    .map((value) => String(value).toLowerCase());

  return values.some((value) => value === 'employee' || value === 'employeeselect');
}

function hasEmployeeFieldName(field: string) {
  const normalized = field.toLowerCase();
  return normalized.includes('employeeid') || normalized.includes('approver');
}

export function extractEmployeeSelectFieldOptions(
  schemaJson?: string,
): BpmEmployeeSelectFieldOption[] {
  const parsed = safeParseSchema(schemaJson);
  const seen = new Set<string>();

  return collectSchemaNodes(parsed)
    .map((node) => {
      const field = typeof node.field === 'string' ? node.field.trim() : '';
      if (!field || seen.has(field)) {
        return undefined;
      }
      if (!hasEmployeeSelectType(node) && !hasEmployeeFieldName(field)) {
        return undefined;
      }

      seen.add(field);
      return {
        field,
        label:
          typeof node.title === 'string' && node.title.trim()
            ? node.title.trim()
            : field,
      };
    })
    .filter(Boolean) as BpmEmployeeSelectFieldOption[];
}
```

- [ ] **Step 4: Wire options into process designer**

In `bpm-process-designer-adapter.vue`, add the import:

```ts
import { extractEmployeeSelectFieldOptions } from './employee-select-field-options';
```

Add the prop:

```ts
    formSchemaJson?: string;
```

Add its default:

```ts
    formSchemaJson: '',
```

Add computed helpers after `selectedNode`:

```ts
const employeeSelectFieldOptions = computed(() =>
  extractEmployeeSelectFieldOptions(props.formSchemaJson),
);

const employeeSelectFieldSet = computed(
  () => new Set(employeeSelectFieldOptions.value.map((item) => item.field)),
);
```

Replace `validate()` with:

```ts
async function validate() {
  if (!nodes.value.length) {
    return {
      message: '请至少保留一个审批节点',
      ok: false,
    };
  }

  const invalidEmployeeSelectNode = nodes.value.find(
    (item) =>
      item.candidateResolverType === 'EMPLOYEE_SELECT_AT_START' &&
      (!item.employeeSelectFieldKey ||
        !employeeSelectFieldSet.value.has(item.employeeSelectFieldKey)),
  );
  if (invalidEmployeeSelectNode) {
    return {
      message: `审批节点【${invalidEmployeeSelectNode.name}】请选择发起时自选审批人字段`,
      ok: false,
    };
  }

  return {
    message: undefined,
    ok: true,
  };
}
```

Replace the current `ElInput` field under `EMPLOYEE_SELECT_AT_START` with:

```vue
            <ElSelect
              v-model="selectedNode.employeeSelectFieldKey"
              :disabled="disabled || readonly || !employeeSelectFieldOptions.length"
              placeholder="请选择表单中的员工字段"
              @change="handleStateChange"
            >
              <ElOption
                v-for="field in employeeSelectFieldOptions"
                :key="field.field"
                :label="field.label"
                :value="field.field"
              />
            </ElSelect>
```

- [ ] **Step 5: Run adapter test to verify pass**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

Expected: PASS with the new extractor tests and existing simple-model contract tests.

- [ ] **Step 6: Commit Task 1**

Run:

```powershell
git add hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/employee-select-field-options.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue
git commit -m "feat: 支持 BPM 自选审批字段候选项"
```

Expected: commit succeeds with only Task 1 files.

---

### Task 2: Model Editor Passes Form Schema To Process Designer

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: `BpmDesignerDetailRecord.formSchemaJson?: null | string`.
- Produces: `BpmProcessDesignerAdapter` receives `:form-schema-json="baseInfo.formSchemaJson"`.

- [ ] **Step 1: Write failing source contract test**

In `bpm-modules.test.ts`, add these assertions to `keeps the model editor on the shared edit baseline and process designer adapter`:

```ts
    expect(modelEditorSource).toContain('formSchemaJson');
    expect(modelEditorSource).toContain(':form-schema-json="baseInfo.formSchemaJson"');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because `model-editor.vue` does not pass `formSchemaJson` to `BpmProcessDesignerAdapter`.

- [ ] **Step 3: Add form schema state and prop wiring**

In `model-editor.vue`, add this property to `DesignerBaseInfo`:

```ts
  formSchemaJson: string;
```

Add the default in `baseInfo`:

```ts
  formSchemaJson: '',
```

Add it in `resetBaseInfo()`:

```ts
    formSchemaJson: '',
```

Add it in `loadDetail()` when assigning `baseInfo`:

```ts
      formSchemaJson: detail.formSchemaJson || '',
```

Add the prop to `BpmProcessDesignerAdapter`:

```vue
            :form-schema-json="baseInfo.formSchemaJson"
```

- [ ] **Step 4: Run module contract test to verify pass**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 5: Commit Task 2**

Run:

```powershell
git add hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: 流程设计器接收 BPM 表单字段 schema"
```

Expected: commit succeeds with only Task 2 files.

---

### Task 3: Runtime Form Employee Single Select

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: form-create `FormRule[]`, employee option rows `{ label: string; value: number }[]`, and a remote method `(keyword: string) => void | Promise<void>`.
- Produces:
  - `export interface BpmRuntimeEmployeeOption { label: string; value: number; }`
  - `export function hasEmployeeSelectRule(rules: FormRule[]): boolean`
  - `export function normalizeRuntimeFormRules(rules: FormRule[], employeeOptions: BpmRuntimeEmployeeOption[], remoteMethod: (keyword: string) => void | Promise<void>): FormRule[]`

- [ ] **Step 1: Write failing runtime rule tests**

Create `bpm-runtime-form-rules.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest';

import {
  hasEmployeeSelectRule,
  normalizeRuntimeFormRules,
} from './bpm-runtime-form-rules';

describe('bpm runtime form rules', () => {
  it('把 employeeSelect 字段归一化为单选员工下拉', () => {
    const remoteMethod = vi.fn();

    const rules = normalizeRuntimeFormRules(
      [
        {
          field: 'approverEmployeeId',
          title: '审批人',
          type: 'employeeSelect',
        } as any,
      ],
      [{ label: '张三', value: 100 }],
      remoteMethod,
    );

    expect(rules).toEqual([
      {
        field: 'approverEmployeeId',
        title: '审批人',
        type: 'select',
        options: [{ label: '张三', value: 100 }],
        props: {
          clearable: true,
          filterable: true,
          multiple: false,
          remote: true,
          remoteMethod,
          reserveKeyword: true,
        },
      },
    ]);
  });

  it('保留普通表单字段不做员工选择归一化', () => {
    const remoteMethod = vi.fn();

    const rules = normalizeRuntimeFormRules(
      [
        {
          field: 'amount',
          title: '金额',
          type: 'inputNumber',
        } as any,
      ],
      [],
      remoteMethod,
    );

    expect(rules).toEqual([
      {
        field: 'amount',
        title: '金额',
        type: 'inputNumber',
      },
    ]);
  });

  it('识别运行时表单是否包含员工选择字段', () => {
    expect(
      hasEmployeeSelectRule([
        {
          field: 'approverEmployeeId',
          title: '审批人',
          type: 'employee',
        } as any,
      ]),
    ).toBe(true);

    expect(
      hasEmployeeSelectRule([
        {
          field: 'amount',
          title: '金额',
          type: 'inputNumber',
        } as any,
      ]),
    ).toBe(false);
  });
});
```

- [ ] **Step 2: Add failing source contract assertion**

In `bpm-modules.test.ts`, add these assertions to `keeps the runtime form renderer compatible with draft schemas wrapped in a fields object`:

```ts
    expect(rendererSource).toContain('normalizeRuntimeFormRules');
    expect(rendererSource).toContain('queryEmployeePage');
```

- [ ] **Step 3: Run tests to verify fail**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because `bpm-runtime-form-rules.ts` does not exist and the renderer does not use `normalizeRuntimeFormRules`.

- [ ] **Step 4: Add runtime rule normalization helper**

Create `bpm-runtime-form-rules.ts`:

```ts
import type { FormRule } from '@form-create/element-ui';

export interface BpmRuntimeEmployeeOption {
  label: string;
  value: number;
}

type RuntimeRule = FormRule & Record<string, any>;

function isEmployeeSelectRule(rule: RuntimeRule) {
  const values = [
    rule.type,
    rule.component,
    rule.props?.type,
    rule.props?.component,
  ]
    .filter(Boolean)
    .map((value) => String(value).toLowerCase());

  return values.some((value) => value === 'employee' || value === 'employeeselect');
}

export function hasEmployeeSelectRule(rules: FormRule[]) {
  return rules.some((rule) => isEmployeeSelectRule(rule as RuntimeRule));
}

export function normalizeRuntimeFormRules(
  rules: FormRule[],
  employeeOptions: BpmRuntimeEmployeeOption[],
  remoteMethod: (keyword: string) => Promise<void> | void,
): FormRule[] {
  return rules.map((rule) => {
    const runtimeRule = rule as RuntimeRule;
    if (!isEmployeeSelectRule(runtimeRule)) {
      return rule;
    }

    return {
      ...runtimeRule,
      options: employeeOptions,
      props: {
        ...(runtimeRule.props ?? {}),
        clearable: true,
        filterable: true,
        multiple: false,
        remote: true,
        remoteMethod,
        reserveKeyword: true,
      },
      type: 'select',
    } as FormRule;
  });
}
```

- [ ] **Step 5: Wire runtime renderer to employee options**

In `bpm-runtime-form-renderer.vue`, update imports:

```ts
import { computed, markRaw, ref, watch } from 'vue';

import { queryEmployeePage } from '#/api/system/organization';

import {
  hasEmployeeSelectRule,
  normalizeRuntimeFormRules,
} from './bpm-runtime-form-rules';
```

Rename the current `formRules` computed to `rawFormRules`:

```ts
const rawFormRules = computed<FormRule[]>(() => {
```

Add employee option state after `formCreateComponent`:

```ts
const employeeOptions = ref<{ label: string; value: number }[]>([]);
```

Add the normalized rules computed after `rawFormRules`:

```ts
const formRules = computed<FormRule[]>(() =>
  normalizeRuntimeFormRules(
    rawFormRules.value,
    employeeOptions.value,
    loadEmployeeOptions,
  ),
);
```

Add the employee option loader before `submit()`:

```ts
async function loadEmployeeOptions(keyword = '') {
  const result = await queryEmployeePage({
    disabledFlag: false,
    keyword,
    pageNum: 1,
    pageSize: 20,
  });
  employeeOptions.value = (result?.list ?? []).map((item) => ({
    label: `${item.actualName}（${item.departmentName || '未分配部门'}）`,
    value: item.employeeId,
  }));
}
```

Add a watcher after `submit()`:

```ts
watch(
  rawFormRules,
  (rules) => {
    if (hasEmployeeSelectRule(rules)) {
      void loadEmployeeOptions();
    }
  },
  { immediate: true },
);
```

The existing template keeps using `:rule="formRules"`.

- [ ] **Step 6: Run runtime rule and module tests to verify pass**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 7: Commit Task 3**

Run:

```powershell
git add hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: 运行时表单支持 BPM 员工单选字段"
```

Expected: commit succeeds with only Task 3 files.

---

### Task 4: Verification And Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-10-bpm-p3-employee-select-usability-acceptance.md`

**Interfaces:**
- Consumes: Tasks 1-3 and P3.1b backend tests.
- Produces: Chinese UTF-8 acceptance record with actual verification evidence.

- [ ] **Step 1: Run frontend adapter contracts**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS. Record test file count, test count, and duration.

- [ ] **Step 2: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS with `vue-tsc --noEmit --skipLibCheck`.

- [ ] **Step 3: Run backend P3.1b boundary tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

Expected: PASS. Record test count and Maven warning text if present.

- [ ] **Step 4: Write acceptance record**

Create `docs/superpowers/specs/2026-07-10-bpm-p3-employee-select-usability-acceptance.md`:

```markdown
# BPM P3.1c 发起时自选审批人可用性补强验收记录

## 结论

BPM P3.1c 发起时自选审批人可用性补强通过源级验收。

本轮将 P3.1b 的 `EMPLOYEE_SELECT_AT_START` 从手输字段 key 推进为表单 schema 字段选择和运行时员工单选。后端仍保持单员工 ID 解析边界，不新增 SQL、不新增依赖、不扩展会签或多人审批。

## 验收范围

- 流程设计器从模型关联表单 `formSchemaJson` 提取员工字段候选项。
- 模型编辑页把 `formSchemaJson` 传给流程设计器。
- 运行时表单把 `employeeSelect` 字段归一化为单员工下拉。
- 后端 P3.1b 单 ID 解析边界保持通过。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 前端 P3.1c 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | 写入 Step 1 终端输出中的 `Test Files`、`Tests`、`Duration` 与 PASS 结论 |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | 写入 Step 2 终端输出和退出码 0 的 PASS 结论 |
| 后端 P3.1b 边界测试 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test` | 写入 Step 3 终端输出中的 `Tests run`、`Total time`、`Finished at` 与 PASS 结论 |

## 边界说明

- 本轮不新增后端字段存在性校验，字段存在性先由前端设计器保证。
- 本轮不支持多人审批、会签、或签、岗位、角色组、表达式或多实例。
- 本轮不新增 SQL。
- 本轮不新增依赖。

## 已知提示

- 如 Maven 仍提示本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 第 235 行附近存在非预期文本，该提示属于既有本机配置问题，门禁通过即可。
```

Write the result rows from the observed Step 1-3 command output before committing.

- [ ] **Step 5: Self-review acceptance record**

Run:

```powershell
rg -n "待[补]|占[位]|稍后[补]|未[填]写|写入 Step" docs/superpowers/specs/2026-07-10-bpm-p3-employee-select-usability-acceptance.md
```

Expected: no matches.

- [ ] **Step 6: Commit Task 4**

Run:

```powershell
git add docs/superpowers/specs/2026-07-10-bpm-p3-employee-select-usability-acceptance.md
git commit -m "docs: 增加 BPM 发起时自选审批人可用性验收记录"
```

Expected: commit succeeds with only the acceptance record.

---

## Final Verification

- [ ] Run `git status --short --branch`.
- [ ] Run `git diff --name-only HEAD~4..HEAD` and confirm no SQL or dependency files changed.
- [ ] Summarize commits and verification evidence in Chinese.
- [ ] State explicitly that P3.1c did not add SQL, dependencies, backend validator signature changes, or multi-employee approval.

## Plan Self-Review

- Spec coverage: covered field selector, schema passing, runtime employee single select, backend boundary tests, no SQL, no dependencies, no backend validator signature expansion, and no multi-employee approval.
- Placeholder scan: plan avoids permanent placeholder strings; the acceptance record self-review scans for copied instruction text before commit.
- Type consistency: `formSchemaJson`, `employeeSelectFieldKey`, `employeeSelect`, `BpmEmployeeSelectFieldOption`, and `normalizeRuntimeFormRules` are used consistently across tasks.
