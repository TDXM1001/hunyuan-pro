# BPM 管理端基座 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前分支把 `hunyuan-system` 的 BPM 管理端补齐为当前系统风格的 8 个交付块能力面，完成分类、表单、模型、定义、实例、任务、监听器、设计器的一期基座闭环。

**Architecture:** 保持当前 `Page + ArtSearchPanel + ArtTablePanel + ArtEditPage` 的业务壳层不变，把参考仓中的表单设计器与流程设计器只作为隐藏内核接入 `adapter` 层。流程设计继续以当前后端 `simpleModelJson + startRuleJson + variableMappingJson` 为保存真相，前端通过受约束的设计器适配层把图形设计结果映射回现有 JSON contract，而不是前端自造另一套保存语义。

**Tech Stack:** Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, vue-tsc, pnpm, `@form-create/designer`, `@form-create/element-ui`, `bpmn-js`, `bpmn-js-properties-panel`

## Global Constraints

- 遵循 `AGENTS.md`：一次只推进一个可验证增量。
- 遵循 `AGENTS.md`：编辑前先说明为什么需要改动。
- 遵循 `AGENTS.md`：优先复用当前项目模式，不新增无必要依赖。
- 当前任务在 `main` 分支执行，且当前工作区存在大量无关脏文件；每次提交只能包含本任务对应文件。
- 所有新增或修改文件使用 UTF-8。
- 页面文案、注释、提交信息使用中文。
- 所有列表页遵循 `docs/frontend-list-table-page-standard.md`。
- 所有编辑页、详情页、抽屉详情遵循 `docs/frontend-edit-detail-page-standard.md`。
- 不复制参考仓 `ContentWrap`、页头语义、间距体系或二套管理页壳层。
- 第三方设计器依赖只允许出现在 `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/` 及其内层帮助文件。
- 业务页不得直接访问 `fc-designer`、`bpmn-js`、`window.bpmnInstances`。
- 表单设计器第三方依赖属于 P0 硬需求，不走自研表单设计器路线。
- 流程设计器前端保存真相必须继续对齐当前后端 `BpmDesignerSaveForm`，不允许前端直接改成只传 BPMN XML。
- `定义 / 实例 / 任务` 的详情页必须采用 `列表 + 右侧详情抽屉 + Tab 分面`。
- `表单 / 模型` 的主设计入口必须从列表行操作进入隐藏路由页，不新增一级菜单。
- 不前端自造 `form category`、`task admin action`、`listener detail` 等业务语义；只能消费当前 `/bpm/*` contract 或后端配套 BPM 底座计划已经明确的新 contract。
- 本计划中的 `实例详情 / 任务详情 / 监听器管理闭环` 依赖配套后端 BPM 底座计划提供对应 admin endpoints；如果这些接口未落地，不允许在前端用假数据或页面私有 shim 硬凑闭环。
- 本次实现完成后必须至少验证：
  - `pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom`
  - `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck`
  - `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck`

---

## File Structure

### Source Contracts and Hidden Routes

- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
  - 扩展 8 个交付块页面、3 个详情抽屉、2 个隐藏设计器路由的源码契约断言。
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
  - 扩展表单设计快照、流程设计草稿、运行态详情、监听器管理的新 API contract 断言。
- Modify: `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
  - 新增表单设计器隐藏路由，收口模型设计器隐藏路由命名和路径。

### Shared BPM Adapter Layer

- Modify: `hunyuan-design/apps/hunyuan-system/package.json`
  - 引入最小设计器依赖，只挂到 `@hunyuan/system`。
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
  - 定义表单设计器、流程设计器、SimpleModel 节点草稿的标准接口。
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts`
  - 在当前后端 `simpleModelJson` 与前端受约束流程设计草稿之间做双向转换。
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-form-designer-adapter.vue`
  - 封装 `@form-create/designer`，输出 `schemaJson/layoutJson` 快照。
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
  - 封装 `bpmn-js` 与右侧属性编辑区，输出 `simpleModelJson` 与只读 BPMN XML 预览。
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
  - 校验 adapter 暴露方法、SimpleModel 桥接、脏状态和销毁行为。

### Form Module

- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/form.ts`
  - 增加表单设计快照 helper，统一空白快照和设计态 payload。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-list.vue`
  - 从“元数据 + JSON 文本框”改为“元数据管理 + 设计入口”。
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue`
  - 新的表单设计页，外层使用当前系统编辑页壳层，内部挂 `BpmFormDesignerAdapter`。

### Model Module

- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/model.ts`
  - 增加流程设计草稿 helper，收口 `simpleModelJson` 的默认值和设计器映射。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-list.vue`
  - 把“设计器”行操作升级为真实设计页入口，并增强草稿状态感知。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
  - 从多段 JSON 文本框升级为“元数据 + 图形设计工作区 + 高级规则区”的编辑页。

### Definition / Instance / Task Runtime Surface

- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`
  - 收口定义详情字段的格式化 helper。
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
  - 增加实例详情、任务详情、任务动作时间线的接口与类型。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue`
  - 改为 `useVbenDrawer` 打开详情抽屉。
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/components/definition-detail-drawer.vue`
  - 定义详情抽屉，Tab 包含基础信息、部署与版本、流程图/XML、关联表单。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
  - 增加“详情”操作和右侧详情抽屉。
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/components/instance-detail-drawer.vue`
  - 实例详情抽屉，Tab 包含基础信息、流程图、表单快照、变量、审批轨迹。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`
  - 增加“详情”操作和右侧详情抽屉。
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/components/task-detail-drawer.vue`
  - 任务详情抽屉，Tab 包含基础信息、所属实例、表单快照、变量、流转记录。

### Listener Module

- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/listener.ts`
  - 从只读目录扩展到管理闭环 contract。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue`
  - 升级为标准管理列表页，提供查询、新增、编辑、状态管理。

## Task 1: Lock the BPM Management Surface and Hidden Designer Routes

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue`

**Interfaces:**
- Consumes:
  - Existing pages under `src/views/system/bpm/*`
  - `RouteRecordRaw[]` from `vue-router`
  - `existsSync`, `readFileSync`, `resolve`
- Produces:
  - Hidden route `/system/bpm/form/designer`
  - Hidden route `/system/bpm/model/designer`
  - Page component `SystemBpmFormDesigner`
  - Source contract asserting real local view files for 8 个交付块与 2 个设计器路由

- [ ] **Step 1: Extend the BPM source-contract test with the full P0 surface**

Append these path constants to `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`:

```ts
const formDesignerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue';
const modelDesignerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue';
const instancePagePath =
  'apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue';
const taskPagePath =
  'apps/hunyuan-system/src/views/system/bpm/task/task-list.vue';
const listenerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue';
const bpmRoutePath = 'apps/hunyuan-system/src/router/routes/static/bpm.ts';
```

Add these assertions:

```ts
it('为表单和模型提供隐藏设计器路由页', () => {
  [formDesignerPagePath, modelDesignerPagePath].forEach((path) => {
    expect(existsSync(resolve(process.cwd(), path))).toBe(true);
  });

  const routeSource = readFileSync(resolve(process.cwd(), bpmRoutePath), 'utf8');
  expect(routeSource).toContain('/system/bpm/form/designer');
  expect(routeSource).toContain('/system/bpm/model/designer');
  expect(routeSource).toContain('hideInMenu: true');
});

it('让 BPM 管理面覆盖分类、表单、模型、定义、实例、任务、监听器七个列表入口', () => {
  [categoryPagePath, definitionPagePath, instancePagePath, runtimePagePath, listenerPagePath]
    .forEach((path) => {
      const source = readFileSync(resolve(process.cwd(), path), 'utf8');
      expect(source).toContain('ArtSearchPanel');
      expect(source).toContain('ArtTablePanel');
      expect(source).toContain('ArtTableHeader');
      expect(source).toContain('ArtTable');
    });
});

it('让新的表单设计页继续沿用现有编辑页基座', () => {
  const source = readFileSync(resolve(process.cwd(), formDesignerPagePath), 'utf8');

  expect(source).toContain('ArtEditPage');
  expect(source).toContain('ArtEditSection');
  expect(source).toContain('SystemBpmFormDesigner');
});
```

- [ ] **Step 2: Run the focused source-contract test and confirm it fails**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:
- The run fails because `form-designer.vue` does not exist yet.
- The route contract fails because `/system/bpm/form/designer` is not yet declared.

- [ ] **Step 3: Add the hidden designer route and the form-designer shell page**

Update `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`:

```ts
const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/system/bpm/form/form-designer.vue'),
    meta: {
      hideInMenu: true,
      title: '表单设计器',
    },
    name: 'SystemBpmFormDesignerRoute',
    path: '/system/bpm/form/designer',
  },
  {
    component: () => import('#/views/system/bpm/model/model-editor.vue'),
    meta: {
      hideInMenu: true,
      title: '流程设计器',
    },
    name: 'SystemBpmModelDesignerRoute',
    path: '/system/bpm/model/designer',
  },
];
```

Create `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue` with this shell:

```vue
<script setup lang="ts">
import { computed, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElSwitch,
  ElTag,
} from 'element-plus';

defineOptions({ name: 'SystemBpmFormDesigner' });

const route = useRoute();
const router = useRouter();

const formId = computed(() => Number(route.query.formId || 0));
const formData = reactive({
  disabledFlag: false,
  formKey: '',
  formName: '',
  remark: '',
});

function handleBack() {
  void router.push('/system/bpm/form');
}
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtEditPage title="表单设计器">
      <template #back>
        <ElButton link type="primary" @click="handleBack">返回表单列表</ElButton>
      </template>

      <template #extra>
        <ElTag effect="light" round type="info">
          {{ formId ? `表单 ID：${formId}` : '新建设计草稿' }}
        </ElTag>
      </template>

      <ElForm :model="formData" class="form-designer-page__form" label-position="top">
        <ArtEditSection title="表单信息" :index="1">
          <ElFormItem label="表单编码">
            <ElInput v-model="formData.formKey" disabled />
          </ElFormItem>
          <ElFormItem label="表单名称">
            <ElInput v-model="formData.formName" disabled />
          </ElFormItem>
          <ElFormItem label="禁用状态">
            <ElSwitch v-model="formData.disabledFlag" disabled />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="备注">
            <ElInput v-model="formData.remark" disabled type="textarea" />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="设计工作区" :index="2">
          <div class="form-designer-page__workbench">适配层工作区</div>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.form-designer-page__form {
  display: grid;
  gap: 12px;
}

.form-designer-page__workbench {
  align-items: center;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  display: flex;
  height: 520px;
  justify-content: center;
}
</style>
```

- [ ] **Step 4: Re-run the source-contract test and confirm the route surface is now anchored**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:
- The hidden designer route assertions pass.
- The new form-designer shell is recognized as a real local view file.

- [ ] **Step 5: Commit the route and source-guard slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue
git commit -m "docs: 锁定 BPM 管理端页面与隐藏设计器路由契约"
```

Expected:
- Git creates one commit containing only the hidden-route anchoring and source-contract updates.

## Task 2: Land the Shared BPM Adapter Layer and Dependency Spike

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/package.json`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-form-designer-adapter.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`

**Interfaces:**
- Consumes:
  - `SimpleModelValidator` / `SimpleModelBpmnCompiler` already约定的 `nodes[]` 结构
  - `@form-create/designer`
  - `@form-create/element-ui`
  - `bpmn-js`
  - `bpmn-js-properties-panel`
- Produces:
  - `BpmFormDesignerSnapshot`
  - `BpmProcessNodeDraft`
  - `BpmProcessDesignerSnapshot`
  - `BpmFormDesignerExpose`
  - `BpmProcessDesignerExpose`
  - `parseSimpleModelDraft(jsonText: string): BpmProcessNodeDraft[]`
  - `stringifySimpleModelDraft(nodes: BpmProcessNodeDraft[]): string`
  - `buildReadonlyBpmnXml(modelKey: string, modelName: string, nodes: BpmProcessNodeDraft[]): string`

- [ ] **Step 1: Add failing adapter contract tests**

Create `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`:

```ts
import { describe, expect, it } from 'vitest';

import {
  buildReadonlyBpmnXml,
  parseSimpleModelDraft,
  stringifySimpleModelDraft,
} from './simple-model-bridge';

describe('bpm designer adapters', () => {
  it('把后端 simpleModelJson 解析为受约束的节点草稿数组', () => {
    expect(
      parseSimpleModelDraft(
        JSON.stringify({
          nodes: [
            {
              approvalMode: 'single',
              candidateResolverType: 'EMPLOYEE',
              id: 'task_apply',
              listeners: [{ channels: ['MESSAGE'], listenerCode: 'notify_message' }],
              name: '部门负责人审批',
              type: 'userTask',
            },
          ],
        }),
      ),
    ).toEqual([
      {
        approvalMode: 'single',
        candidateResolverType: 'EMPLOYEE',
        id: 'task_apply',
        listeners: [{ channels: ['MESSAGE'], listenerCode: 'notify_message' }],
        name: '部门负责人审批',
        nodeKey: 'task_apply',
        type: 'userTask',
      },
    ]);
  });

  it('把节点草稿重新序列化为当前后端可接受的 simpleModelJson', () => {
    expect(
      stringifySimpleModelDraft([
        {
          approvalMode: 'single',
          candidateResolverType: 'ROLE',
          id: 'task_finance',
          listeners: [],
          name: '财务审批',
          nodeKey: 'task_finance',
          type: 'userTask',
        },
      ]),
    ).toBe(
      '{"nodes":[{"id":"task_finance","nodeKey":"task_finance","name":"财务审批","type":"userTask","approvalMode":"single","candidateResolverType":"ROLE","listeners":[]}]}',
    );
  });

  it('根据顺序审批节点生成只读 BPMN XML 预览', () => {
    const xml = buildReadonlyBpmnXml('leave_apply', '请假流程', [
      {
        approvalMode: 'single',
        candidateResolverType: 'DEPARTMENT_MANAGER',
        id: 'task_manager',
        listeners: [],
        name: '主管审批',
        nodeKey: 'task_manager',
        type: 'userTask',
      },
    ]);

    expect(xml).toContain('<process id="leave_apply"');
    expect(xml).toContain('<userTask id="task_manager"');
    expect(xml).toContain('flowable:assignee="${assignee_task_manager}"');
  });
});
```

Also append these API contract assertions to `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`:

```ts
{
  label: 'form',
  needles: [
    '/bpm/form/query',
    '/bpm/form/add',
    '/bpm/form/update',
    'buildEmptyBpmFormDesignerSnapshot',
  ],
  path: 'apps/hunyuan-system/src/api/system/bpm/form.ts',
},
{
  label: 'model',
  needles: [
    '/bpm/model/query',
    '/bpm/designer/detail/',
    '/bpm/designer/save',
    'buildEmptyBpmDesignerDraft',
  ],
  path: 'apps/hunyuan-system/src/api/system/bpm/model.ts',
},
```

- [ ] **Step 2: Run the adapter and API contract tests and confirm they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected:
- The adapter test fails because `simple-model-bridge.ts` does not exist.
- The API contract test fails because helper names are still absent from `form.ts` and `model.ts`.

- [ ] **Step 3: Add the minimum designer dependencies to `@hunyuan/system`**

Run:

```bash
pnpm --dir hunyuan-design --filter @hunyuan/system add @form-create/designer @form-create/element-ui bpmn-js bpmn-js-properties-panel
```

Expected:
- `hunyuan-design/apps/hunyuan-system/package.json` gains the four runtime dependencies.
- No workspace-level shared package is modified except the generated lockfile.

- [ ] **Step 4: Implement the adapter contracts and SimpleModel bridge**

Create `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`:

```ts
export interface BpmListenerBinding {
  channels: string[];
  listenerCode: string;
}

export interface BpmFormDesignerSnapshot {
  layoutJson: string;
  schemaJson: string;
}

export interface BpmProcessNodeDraft {
  approvalMode?: 'single' | 'singleOnly';
  candidateResolverType?: 'DEPARTMENT_MANAGER' | 'EMPLOYEE' | 'ROLE';
  id: string;
  listeners: BpmListenerBinding[];
  name: string;
  nodeKey: string;
  type: 'userTask';
}

export interface BpmProcessDesignerSnapshot {
  bpmnXml: string;
  nodes: BpmProcessNodeDraft[];
}

export interface BpmFormDesignerExpose {
  getSnapshot: () => BpmFormDesignerSnapshot;
  isDirty: () => boolean;
  load: (snapshot: Partial<BpmFormDesignerSnapshot>) => Promise<void>;
  resetDirty: () => void;
  validate: () => Promise<{ message?: string; ok: boolean }>;
}

export interface BpmProcessDesignerExpose {
  getSnapshot: () => BpmProcessDesignerSnapshot;
  isDirty: () => boolean;
  load: (snapshot: Partial<BpmProcessDesignerSnapshot>) => Promise<void>;
  resetDirty: () => void;
  validate: () => Promise<{ message?: string; ok: boolean }>;
}
```

Create `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts`:

```ts
import type { BpmProcessNodeDraft } from './types';

function normalizeNode(rawNode: Record<string, any>): BpmProcessNodeDraft {
  const nodeKey = String(rawNode.nodeKey || rawNode.id || '').trim();
  const nodeId = String(rawNode.id || rawNode.nodeKey || '').trim() || nodeKey;

  return {
    approvalMode: rawNode.approvalMode || 'single',
    candidateResolverType: rawNode.candidateResolverType || rawNode.resolverType || 'EMPLOYEE',
    id: nodeId,
    listeners: Array.isArray(rawNode.listeners) ? rawNode.listeners : [],
    name: String(rawNode.name || '审批节点').trim(),
    nodeKey: nodeKey || nodeId,
    type: 'userTask',
  };
}

export function parseSimpleModelDraft(jsonText: string): BpmProcessNodeDraft[] {
  if (!jsonText.trim()) {
    return [];
  }

  const parsed = JSON.parse(jsonText);
  const nodes = Array.isArray(parsed?.nodes) ? parsed.nodes : [];
  return nodes
    .filter((item) => item && (item.type || 'userTask') === 'userTask')
    .map((item) => normalizeNode(item));
}

export function stringifySimpleModelDraft(nodes: BpmProcessNodeDraft[]): string {
  return JSON.stringify({
    nodes: nodes.map((node) => ({
      id: node.id,
      nodeKey: node.nodeKey,
      name: node.name,
      type: 'userTask',
      approvalMode: node.approvalMode || 'single',
      candidateResolverType: node.candidateResolverType || 'EMPLOYEE',
      listeners: node.listeners || [],
    })),
  });
}

export function buildReadonlyBpmnXml(
  modelKey: string,
  modelName: string,
  nodes: BpmProcessNodeDraft[],
) {
  const escapedKey = modelKey || 'process_model';
  const escapedName = modelName || '流程模型';
  const taskXml = nodes
    .map(
      (node, index) => `
        <userTask id="${node.nodeKey}" name="${node.name}" flowable:assignee="\${assignee_${node.nodeKey}}"/>
        <sequenceFlow id="flow_${index}" sourceRef="${index === 0 ? 'startEvent' : nodes[index - 1]!.nodeKey}" targetRef="${node.nodeKey}"/>`,
    )
    .join('');
  const endSource = nodes.length ? nodes[nodes.length - 1]!.nodeKey : 'startEvent';

  return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://hunyuan.sa/bpm">
  <process id="${escapedKey}" name="${escapedName}" isExecutable="true">
    <startEvent id="startEvent" name="开始"/>
    ${taskXml}
    <endEvent id="endEvent" name="结束"/>
    <sequenceFlow id="flow_end" sourceRef="${endSource}" targetRef="endEvent"/>
  </process>
</definitions>`;
}
```

- [ ] **Step 5: Implement the two adapter components**

Create `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-form-designer-adapter.vue` with these required behaviors:

```vue
<script setup lang="ts">
import type { BpmFormDesignerExpose, BpmFormDesignerSnapshot } from './types';

import FcDesigner from '@form-create/designer';
import { nextTick, ref } from 'vue';

defineOptions({ name: 'BpmFormDesignerAdapter' });

const emit = defineEmits<{
  change: [snapshot: BpmFormDesignerSnapshot];
  ready: [];
}>();

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    initialSnapshot?: Partial<BpmFormDesignerSnapshot>;
    readonly?: boolean;
  }>(),
  {
    disabled: false,
    initialSnapshot: () => ({}),
    readonly: false,
  },
);

const designerRef = ref<any>();
const dirty = ref(false);

function getSnapshot(): BpmFormDesignerSnapshot {
  return {
    layoutJson: JSON.stringify(designerRef.value?.getOption?.() || {}),
    schemaJson: JSON.stringify(designerRef.value?.getRule?.() || []),
  };
}

async function load(snapshot: Partial<BpmFormDesignerSnapshot>) {
  await nextTick();
  const rules = snapshot.schemaJson ? JSON.parse(snapshot.schemaJson) : [];
  const options = snapshot.layoutJson ? JSON.parse(snapshot.layoutJson) : {};
  designerRef.value?.setRule?.(rules);
  designerRef.value?.setOption?.(options);
  dirty.value = false;
}

async function validate() {
  return { ok: true };
}

function resetDirty() {
  dirty.value = false;
}

function isDirty() {
  return dirty.value;
}

defineExpose<BpmFormDesignerExpose>({
  getSnapshot,
  isDirty,
  load,
  resetDirty,
  validate,
});
</script>
```

Create `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue` with these required behaviors:

```vue
<script setup lang="ts">
import type {
  BpmProcessDesignerExpose,
  BpmProcessDesignerSnapshot,
  BpmProcessNodeDraft,
} from './types';

import BpmnModeler from 'bpmn-js/lib/Modeler';
import { computed, nextTick, reactive, ref } from 'vue';

import {
  buildReadonlyBpmnXml,
  parseSimpleModelDraft,
} from './simple-model-bridge';

defineOptions({ name: 'BpmProcessDesignerAdapter' });

const emit = defineEmits<{
  change: [snapshot: BpmProcessDesignerSnapshot];
  ready: [];
}>();

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    initialSnapshot?: Partial<BpmProcessDesignerSnapshot>;
    modelKey?: string;
    modelName?: string;
    readonly?: boolean;
  }>(),
  {
    disabled: false,
    initialSnapshot: () => ({}),
    modelKey: '',
    modelName: '',
    readonly: false,
  },
);

const canvasRef = ref<HTMLDivElement>();
const dirty = ref(false);
const selectedNodeId = ref('');
const nodes = ref<BpmProcessNodeDraft[]>([]);
const modeler = ref<any>();

const selectedNode = computed(() =>
  nodes.value.find((item) => item.nodeKey === selectedNodeId.value),
);

async function ensureModeler() {
  if (modeler.value || !canvasRef.value) {
    return;
  }
  modeler.value = new BpmnModeler({ container: canvasRef.value });
}

async function renderCanvas() {
  await ensureModeler();
  await nextTick();
  await modeler.value?.importXML?.(
    buildReadonlyBpmnXml(props.modelKey || 'process_model', props.modelName || '流程模型', nodes.value),
  );
}

async function load(snapshot: Partial<BpmProcessDesignerSnapshot>) {
  nodes.value = snapshot.nodes?.length
    ? snapshot.nodes
    : parseSimpleModelDraft(snapshot?.bpmnXml ? '' : snapshot?.['simpleModelJson' as never] || '{"nodes":[]}');
  selectedNodeId.value = nodes.value[0]?.nodeKey || '';
  await renderCanvas();
  dirty.value = false;
}

function getSnapshot(): BpmProcessDesignerSnapshot {
  return {
    bpmnXml: buildReadonlyBpmnXml(props.modelKey || 'process_model', props.modelName || '流程模型', nodes.value),
    nodes: nodes.value,
  };
}

async function validate() {
  return {
    message: nodes.value.length ? undefined : '请至少保留一个审批节点',
    ok: nodes.value.length > 0,
  };
}

function resetDirty() {
  dirty.value = false;
}

function isDirty() {
  return dirty.value;
}

defineExpose<BpmProcessDesignerExpose>({
  getSnapshot,
  isDirty,
  load,
  resetDirty,
  validate,
});
</script>
```

- [ ] **Step 6: Re-run adapter tests and the required type checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The adapter test passes.
- `bpm-api.test.ts` recognizes the new helper names.
- `@hunyuan/system` typecheck passes with the new adapter files and dependencies.

- [ ] **Step 7: Commit the adapter-layer slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/package.json hunyuan-design/pnpm-lock.yaml hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-form-designer-adapter.vue hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
git commit -m "feat: 新增 BPM 设计器适配层与依赖基座"
```

Expected:
- Git creates one commit containing only the designer dependency spike and adapter layer.

## Task 3: Implement Unified Detail Drawers for 定义 / 实例 / 任务

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/components/definition-detail-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/components/instance-detail-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/components/task-detail-drawer.vue`

**Interfaces:**
- Consumes:
  - `useVbenDrawer` from `@vben/common-ui`
  - `ArtDetail` from `@vben/art-hooks/detail`
  - Existing `getBpmDefinitionDetail(definitionId: number)`
  - Companion backend contract for:
    - `getBpmInstanceDetail(instanceId: number)`
    - `getBpmTaskDetail(taskId: number)`
    - `queryBpmTaskActionLogs(taskId: number)`
- Produces:
  - `SystemBpmDefinitionDetailDrawer`
  - `SystemBpmInstanceDetailDrawer`
  - `SystemBpmTaskDetailDrawer`
  - `BpmInstanceDetailRecord`
  - `BpmTaskDetailRecord`
  - `BpmTaskActionLogRecord`

- [ ] **Step 1: Extend runtime API tests and drawer source contracts**

Append these needles to `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`:

```ts
{
  label: 'definition',
  needles: [
    '/bpm/definition/query',
    '/bpm/definition/detail/',
    'formatBpmDefinitionDetailJson',
  ],
  path: 'apps/hunyuan-system/src/api/system/bpm/definition.ts',
},
{
  label: 'runtime',
  needles: [
    '/bpm/instance/query',
    '/bpm/instance/detail/',
    '/bpm/task/query',
    '/bpm/task/detail/',
    '/bpm/task/actionLog/',
  ],
  path: 'apps/hunyuan-system/src/api/system/bpm/runtime.ts',
},
```

Append these source assertions to `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`:

```ts
expect(source).toContain('useVbenDrawer');
expect(source).toContain('DefinitionDetailDrawer');
expect(source).toContain('InstanceDetailDrawer');
expect(source).toContain('TaskDetailDrawer');
```

Check these new files:

```ts
const definitionDrawerPath =
  'apps/hunyuan-system/src/views/system/bpm/definition/components/definition-detail-drawer.vue';
const instanceDrawerPath =
  'apps/hunyuan-system/src/views/system/bpm/instance/components/instance-detail-drawer.vue';
const taskDrawerPath =
  'apps/hunyuan-system/src/views/system/bpm/task/components/task-detail-drawer.vue';
```

- [ ] **Step 2: Run the targeted tests and confirm they fail before the drawer refactor**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:
- The runtime API contract fails because detail/action-log helpers are absent.
- The source contract fails because the three drawer component files do not exist yet.

- [ ] **Step 3: Implement the API helpers for detail drawers**

Update `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`:

```ts
export interface BpmTaskActionLogRecord {
  actionAt?: null | string;
  actionType: string;
  actorNameSnapshot?: null | string;
  commentText?: null | string;
  fromAssigneeNameSnapshot?: null | string;
  toAssigneeNameSnapshot?: null | string;
}

export interface BpmInstanceDetailRecord extends BpmInstanceRecord {
  categoryNameSnapshot?: null | string;
  currentFormDataSnapshotJson?: null | string;
  currentNodeSummaryJson?: null | string;
  definitionKeySnapshot?: null | string;
  definitionVersionSnapshot?: null | number;
  initialFormDataSnapshotJson?: null | string;
  summary?: null | string;
}

export interface BpmTaskDetailRecord extends BpmTaskRecord {
  categoryNameSnapshot?: null | string;
  currentFormDataSnapshotJson?: null | string;
  definitionKeySnapshot?: null | string;
  runtimeAssignmentSnapshotJson?: null | string;
}

export async function getBpmInstanceDetail(instanceId: number) {
  return requestClient.get<BpmInstanceDetailRecord>(`/bpm/instance/detail/${instanceId}`);
}

export async function getBpmTaskDetail(taskId: number) {
  return requestClient.get<BpmTaskDetailRecord>(`/bpm/task/detail/${taskId}`);
}

export async function queryBpmTaskActionLogs(taskId: number) {
  return requestClient.get<BpmTaskActionLogRecord[]>(`/bpm/task/actionLog/${taskId}`);
}
```

Update `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`:

```ts
export function formatBpmDefinitionDetailJson(value?: null | string) {
  return value?.trim() || '{}';
}
```

- [ ] **Step 4: Create the three detail drawers and connect the list pages**

Create `definition-detail-drawer.vue` with this contract:

```vue
<script setup lang="ts">
import type { BpmDefinitionRecord } from '#/api/system/bpm';
import type { DetailSection } from '@vben/art-hooks/detail';

import { computed, ref, watch } from 'vue';

import { ArtDetail } from '@vben/art-hooks/detail';
import { useVbenDrawer } from '@vben/common-ui';

import { ElCard, ElInput, ElSkeleton, ElTabs, ElTabPane } from 'element-plus';

import {
  formatBpmDefinitionDetailJson,
  getBpmDefinitionDetail,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmDefinitionDetailDrawer' });

const props = defineProps<{ definition?: BpmDefinitionRecord }>();
const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));
```

The `sections` block must include:

```ts
[
  {
    key: 'basic',
    title: '基础信息',
    items: [
      { label: '定义编码', prop: 'definitionKey' },
      { label: '定义名称', prop: 'definitionName' },
      { label: '版本', prop: 'definitionVersion' },
      { label: '分类', prop: 'categoryNameSnapshot' },
      { label: '表单', prop: 'formNameSnapshot' },
      { label: '发布时间', prop: 'publishedAt' },
    ],
  },
]
```

Create `instance-detail-drawer.vue` and `task-detail-drawer.vue` with the same `useVbenDrawer` pattern, then use `ElTabs` with:

```ts
['基础信息', '流程图', '表单快照', '变量', '审批轨迹']
```

for instance, and:

```ts
['基础信息', '所属实例', '表单快照', '变量', '流转记录']
```

for task.

Each drawer body must keep the current-system density:

```vue
<Drawer class="w-[1180px] max-w-[calc(100vw-24px)]" close-icon-placement="left" content-class="!p-0" :footer="false">
  <div class="bpm-detail-drawer">
    <ElCard class="bpm-detail-drawer__card" shadow="never">
      <ElTabs class="bpm-detail-drawer__tabs" model-value="basic">
        <ElTabPane label="基础信息" name="basic">
          <ArtDetail :data="detailData || {}" :sections="sections" :columns="3" :label-width="112" />
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </div>
</Drawer>
```

Modify the three list pages to adopt `useVbenDrawer`, for example in `definition-list.vue`:

```ts
import { Page, useVbenDrawer } from '@vben/common-ui';
import DefinitionDetailDrawerPanel from './components/definition-detail-drawer.vue';

const selectedDefinition = ref<BpmDefinitionRecord>();

const [DefinitionDetailDrawer, definitionDetailDrawerApi] = useVbenDrawer({
  connectedComponent: DefinitionDetailDrawerPanel,
  destroyOnClose: false,
});

function openDetailDrawer(row: BpmDefinitionRecord) {
  selectedDefinition.value = row;
  definitionDetailDrawerApi.open();
}
```

and render:

```vue
<DefinitionDetailDrawer :definition="selectedDefinition" />
```

- [ ] **Step 5: Verify the drawer contract and runtime type safety**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The source contract sees the three drawer files and `useVbenDrawer`.
- The runtime API contract sees the new detail helpers.
- `@hunyuan/system` typecheck passes.

- [ ] **Step 6: Commit the runtime-observation slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/components/definition-detail-drawer.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/components/instance-detail-drawer.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/components/task-detail-drawer.vue
git commit -m "feat: 统一 BPM 定义实例任务详情抽屉"
```

Expected:
- Git creates one commit containing only the runtime detail drawers and their API/source guards.

## Task 4: Refactor the Form Module into Metadata Management plus Real Designer Page

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/form.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue`

**Interfaces:**
- Consumes:
  - `BpmFormDesignerExpose` from adapter layer
  - Existing `addBpmForm`, `updateBpmForm`, `getBpmFormDetail`
- Produces:
  - `buildEmptyBpmFormDesignerSnapshot(): BpmFormDesignerSnapshot`
  - `buildBpmFormDesignerPayload(snapshot: BpmFormDesignerSnapshot): Pick<BpmFormAddForm, 'layoutJson' | 'schemaJson'>`
  - `SystemBpmFormDesigner`
  - 行操作 `编辑 / 设计`

- [ ] **Step 1: Extend the form API and source contracts**

Append these needles to `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`:

```ts
expect(source).toContain('buildEmptyBpmFormDesignerSnapshot');
expect(source).toContain('buildBpmFormDesignerPayload');
```

Append these source assertions to `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`:

```ts
const formListSource = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/form/form-list.vue'), 'utf8');
expect(formListSource).toContain('openDesigner');
expect(formListSource).toContain('新增表单');
expect(formListSource).toContain('设计');
expect(formListSource).not.toContain('Schema JSON');
expect(formListSource).not.toContain('布局 JSON');
```

- [ ] **Step 2: Run the focused BPM tests and confirm they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:
- The API contract fails because the helper names are absent.
- The page contract fails because `form-list.vue` still exposes JSON textareas and has no `openDesigner`.

- [ ] **Step 3: Add form designer snapshot helpers to `form.ts`**

Update `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/form.ts`:

```ts
import type { BpmFormDesignerSnapshot } from '#/components/bpm/adapters/types';

export function buildEmptyBpmFormDesignerSnapshot(): BpmFormDesignerSnapshot {
  return {
    layoutJson: '{}',
    schemaJson: '[]',
  };
}

export function buildBpmFormDesignerPayload(
  snapshot: BpmFormDesignerSnapshot,
) {
  return {
    layoutJson: snapshot.layoutJson.trim() || '{}',
    schemaJson: snapshot.schemaJson.trim() || '[]',
  };
}
```

- [ ] **Step 4: Refactor `form-list.vue` into metadata-only CRUD plus designer entry**

Make these changes in `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-list.vue`:

1. Keep the dialog fields to:

```ts
disabledFlag
formKey
formName
remark
```

2. Drop `schemaJson` and `layoutJson` from the dialog form and validation rules.

3. Add a row action:

```ts
function openDesigner(row: BpmFormRecord) {
  void router.push({
    path: '/system/bpm/form/designer',
    query: { formId: String(row.formId) },
  });
}
```

4. When creating a new form, seed the payload with an empty snapshot:

```ts
const emptySnapshot = buildEmptyBpmFormDesignerSnapshot();

await addBpmForm({
  ...formData,
  ...buildBpmFormDesignerPayload(emptySnapshot),
});
```

5. Update row actions to:

```vue
<ElButton link size="small" type="primary" @click="openEditDialog(row)">编辑</ElButton>
<ElButton link size="small" type="primary" @click="openDesigner(row)">设计</ElButton>
```

- [ ] **Step 5: Replace the shell `form-designer.vue` with the real adapter-backed work page**

`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue` must:

- load `formId` from query
- call `getBpmFormDetail(formId)`
- keep元数据区只读展示 `formKey / formName / disabledFlag / remark`
- mount `BpmFormDesignerAdapter`
- provide page actions `返回 / 保存 / 预览`

Required script shape:

```ts
import type { BpmFormDesignerExpose, BpmFormDesignerSnapshot } from '#/components/bpm/adapters/types';

import { computed, reactive, ref } from 'vue';

import { ArtPageActions } from '@vben/art-hooks/common';
import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';

import BpmFormDesignerAdapter from '#/components/bpm/adapters/bpm-form-designer-adapter.vue';
import {
  buildBpmFormDesignerPayload,
  getBpmFormDetail,
  updateBpmForm,
} from '#/api/system/bpm';

const designerRef = ref<BpmFormDesignerExpose>();
const previewVisible = ref(false);
const previewSnapshot = ref<BpmFormDesignerSnapshot>();

async function handleSave() {
  const validateResult = await designerRef.value?.validate();
  if (!validateResult?.ok) {
    ElMessage.warning(validateResult?.message || '请先修正表单设计');
    return;
  }

  const snapshot = designerRef.value?.getSnapshot();
  if (!snapshot || !detailData.value) {
    return;
  }

  await updateBpmForm({
    disabledFlag: detailData.value.disabledFlag ?? false,
    formId: detailData.value.formId,
    formKey: detailData.value.formKey,
    formName: detailData.value.formName,
    remark: detailData.value.remark || '',
    ...buildBpmFormDesignerPayload(snapshot),
  });
}

function handlePreview() {
  previewSnapshot.value = designerRef.value?.getSnapshot();
  previewVisible.value = true;
}
```

Required template fragments:

```vue
<template #actions>
  <ArtPageActions :actions="pageActions" />
</template>

<ArtEditSection title="设计工作区" :index="2">
  <BpmFormDesignerAdapter
    ref="designerRef"
    :initial-snapshot="designerSnapshot"
    @ready="handleDesignerReady"
  />
</ArtEditSection>
```

- [ ] **Step 6: Verify the form slice**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The form API helper assertions pass.
- The page contract no longer sees JSON textareas in `form-list.vue`.
- `@hunyuan/system` typecheck passes with the designer page and adapter ref.

- [ ] **Step 7: Commit the form-management slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/form.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue
git commit -m "feat: 接入 BPM 表单管理与设计页"
```

Expected:
- Git creates one commit containing only the form metadata refactor and the real designer page.

## Task 5: Refactor the Model Module into Graphical Process Design plus Advanced Rule Sections

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/model.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`

**Interfaces:**
- Consumes:
  - `BpmProcessDesignerExpose`
  - `parseSimpleModelDraft` / `stringifySimpleModelDraft`
  - Existing `getBpmDesignerDetail`, `saveBpmDesignerDraft`, `validateBpmDesignerDraft`, `simulateBpmDesignerDraft`, `publishBpmDefinition`
- Produces:
  - `buildEmptyBpmDesignerDraft()`
  - `SystemBpmModelEditor`
  - 受约束图形设计工作区 + 保留高级规则 JSON 区

- [ ] **Step 1: Extend the model API and source contracts**

Append these assertions:

```ts
expect(source).toContain('buildEmptyBpmDesignerDraft');
expect(source).toContain('simpleModelJson');
expect(source).toContain('startRuleJson');
expect(source).toContain('variableMappingJson');
```

and in `bpm-modules.test.ts`:

```ts
const modelEditorSource = readFileSync(resolve(process.cwd(), modelEditorPath), 'utf8');
expect(modelEditorSource).toContain('BpmProcessDesignerAdapter');
expect(modelEditorSource).not.toContain('设计器草稿 JSON');
expect(modelEditorSource).not.toContain('请输入 simpleModel JSON');
```

- [ ] **Step 2: Run the focused BPM tests and confirm they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:
- The model API helper assertion fails.
- The source contract fails because `model-editor.vue` still uses plain `simpleModelJson` textarea.

- [ ] **Step 3: Add the empty draft helper to `model.ts`**

Update `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/model.ts`:

```ts
export function buildEmptyBpmDesignerDraft(): BpmDesignerSaveForm {
  return {
    managerScopeJson: '',
    modelId: 0,
    simpleModelJson: '{"nodes":[]}',
    startRuleJson: '{"type":"ALL"}',
    summaryRuleJson: '',
    titleRuleJson: '',
    variableMappingJson: '',
  };
}
```

- [ ] **Step 4: Tighten `model-list.vue` around real designer entry and draft state**

Make these list-level changes:

1. Route to the new hidden path:

```ts
function openDesigner(row: BpmModelRecord) {
  void router.push({
    path: '/system/bpm/model/designer',
    query: { modelId: String(row.modelId) },
  });
}
```

2. Keep metadata dialog for `modelKey / modelName / categoryId / formId / formType / visibleFlag / sort / description`.

3. Preserve row actions as:

```vue
<ElButton link size="small" type="primary" @click="openEditDialog(row)">编辑</ElButton>
<ElButton link size="small" type="primary" @click="openDesigner(row)">设计</ElButton>
<ElButton :loading="publishingId === row.modelId" link size="small" type="success" @click="handlePublish(row)">发布</ElButton>
```

- [ ] **Step 5: Replace the JSON textareas in `model-editor.vue` with adapter-backed graphical design**

The new page must keep the current metadata和高级规则语义，但让 `simpleModelJson` 不再作为主交互文本框。

Required script fragments:

```ts
import type {
  BpmProcessDesignerExpose,
  BpmProcessDesignerSnapshot,
} from '#/components/bpm/adapters/types';

import BpmProcessDesignerAdapter from '#/components/bpm/adapters/bpm-process-designer-adapter.vue';
import {
  parseSimpleModelDraft,
  stringifySimpleModelDraft,
} from '#/components/bpm/adapters/simple-model-bridge';

const designerRef = ref<BpmProcessDesignerExpose>();
const designerNodes = ref([]);

const formData = reactive<BpmDesignerSaveForm>(buildEmptyBpmDesignerDraft());

const designerSnapshot = computed<BpmProcessDesignerSnapshot>(() => ({
  bpmnXml: '',
  nodes: parseSimpleModelDraft(formData.simpleModelJson),
}));

async function handleSave() {
  const validateResult = await designerRef.value?.validate();
  if (!validateResult?.ok) {
    ElMessage.warning(validateResult?.message || '请先修正流程设计');
    return;
  }

  const snapshot = designerRef.value?.getSnapshot();
  if (snapshot) {
    formData.simpleModelJson = stringifySimpleModelDraft(snapshot.nodes);
  }
  await saveBpmDesignerDraft(formData);
}
```

Required template structure:

```vue
<ArtEditSection title="流程设计工作区" :index="2">
  <BpmProcessDesignerAdapter
    ref="designerRef"
    :initial-snapshot="designerSnapshot"
    :model-key="baseInfo.modelKey"
    :model-name="baseInfo.modelName"
  />
</ArtEditSection>

<ArtEditSection title="运行规则" :index="3">
  <ElFormItem class="art-edit-section__full" label="发起规则 JSON" prop="startRuleJson">
    <ElInput v-model="formData.startRuleJson" :rows="6" type="textarea" />
  </ElFormItem>
  <ElFormItem class="art-edit-section__full" label="主管范围 JSON" prop="managerScopeJson">
    <ElInput v-model="formData.managerScopeJson" :rows="4" type="textarea" />
  </ElFormItem>
  <ElFormItem class="art-edit-section__full" label="标题规则 JSON" prop="titleRuleJson">
    <ElInput v-model="formData.titleRuleJson" :rows="4" type="textarea" />
  </ElFormItem>
  <ElFormItem class="art-edit-section__full" label="摘要规则 JSON" prop="summaryRuleJson">
    <ElInput v-model="formData.summaryRuleJson" :rows="4" type="textarea" />
  </ElFormItem>
  <ElFormItem class="art-edit-section__full" label="变量映射 JSON" prop="variableMappingJson">
    <ElInput v-model="formData.variableMappingJson" :rows="6" type="textarea" />
  </ElFormItem>
</ArtEditSection>
```

This preserves the current backend `BpmDesignerSaveForm` truth while moving the core process drafting interaction into the adapter.

- [ ] **Step 6: Verify the model slice**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The model API helper assertion passes.
- The source contract sees `BpmProcessDesignerAdapter` and no longer sees the old `请输入 simpleModel JSON` placeholder.
- `@hunyuan/system` typecheck passes.

- [ ] **Step 7: Commit the model-design slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/model.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue
git commit -m "feat: 接入 BPM 流程设计工作区"
```

Expected:
- Git creates one commit containing only the model list/editor refactor and related BPM contracts.

## Task 6: Complete Listener Management in Current-System Style and Keep Category Stable

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/listener.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue`

**Interfaces:**
- Consumes:
  - Existing `queryBpmListenerChannelOptions`
  - Companion backend listener management endpoints
- Produces:
  - `BpmListenerPageQueryParams`
  - `BpmListenerMutationForm`
  - `queryBpmListenerPage`
  - `getBpmListenerDetail`
  - `addBpmListener`
  - `updateBpmListener`
  - `toggleBpmListenerDisabled`
  - `SystemBpmListenerCatalog` 标准管理列表页

- [ ] **Step 1: Extend the listener API contract test**

Replace the existing listener block in `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts` with:

```ts
{
  label: 'listener',
  needles: [
    '/bpm/listener/query',
    '/bpm/listener/detail/',
    '/bpm/listener/add',
    '/bpm/listener/update',
    '/bpm/listener/updateDisabled/',
    '/bpm/listener/channelOptions',
  ],
  path: 'apps/hunyuan-system/src/api/system/bpm/listener.ts',
},
```

Update the source contract to assert that `listener-catalog.vue` now contains:

```ts
expect(source).toContain('新增监听器');
expect(source).toContain('编辑');
expect(source).toContain('状态');
expect(source).toContain('ArtTableHeader');
```

- [ ] **Step 2: Run the focused BPM tests and confirm they fail**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:
- The listener API contract fails because CRUD helpers are absent.
- The source contract fails because `listener-catalog.vue` is still a只读目录页。

- [ ] **Step 3: Expand `listener.ts` to a real management contract**

Update `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/listener.ts`:

```ts
import type { PageResult } from '#/api/system/organization';

export interface BpmListenerRecord {
  channels: string[];
  disabledFlag?: boolean;
  listenerCode: string;
  listenerName: string;
  remark?: null | string;
}

export interface BpmListenerPageQueryParams {
  channel?: null | string;
  disabledFlag?: boolean;
  keyword?: null | string;
  pageNum: number;
  pageSize: number;
}

export interface BpmListenerMutationForm {
  channels: string[];
  disabledFlag?: boolean;
  listenerCode: string;
  listenerName: string;
  remark?: null | string;
}

export async function queryBpmListenerPage(params: BpmListenerPageQueryParams) {
  return requestClient.post<PageResult<BpmListenerRecord>>('/bpm/listener/query', {
    channel: params.channel?.trim() || undefined,
    disabledFlag: params.disabledFlag,
    keyword: params.keyword?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  });
}

export async function getBpmListenerDetail(listenerCode: string) {
  return requestClient.get<BpmListenerRecord>(
    `/bpm/listener/detail/${encodeURIComponent(listenerCode.trim())}`,
  );
}

export async function addBpmListener(params: BpmListenerMutationForm) {
  return requestClient.post<string>('/bpm/listener/add', params);
}

export async function updateBpmListener(params: BpmListenerMutationForm) {
  return requestClient.post<string>('/bpm/listener/update', params);
}

export async function toggleBpmListenerDisabled(
  listenerCode: string,
  disabledFlag: boolean,
) {
  return requestClient.get<string>(
    `/bpm/listener/updateDisabled/${encodeURIComponent(listenerCode.trim())}/${disabledFlag}`,
  );
}
```

- [ ] **Step 4: Upgrade `listener-catalog.vue` into a standard management page**

Retain the current page path, but refactor it to the same dense CRUD shell used by `category-list.vue`:

- search fields: `keyword / channel / disabledFlag`
- toolbar primary action: `新增监听器`
- columns: `listenerCode / listenerName / channels / disabledFlag / remark / updateTime / actions`
- actions: `编辑 / 启停`
- dialog form fields: `listenerCode / listenerName / channels / disabledFlag / remark`

Required row-actions block:

```vue
<ElSpace class="listener-page__actions">
  <ElButton link size="small" type="primary" @click="openEditDialog(row)">编辑</ElButton>
  <ElButton
    link
    size="small"
    :type="row.disabledFlag ? 'success' : 'warning'"
    @click="handleToggleDisabled(row)"
  >
    {{ row.disabledFlag ? '启用' : '停用' }}
  </ElButton>
</ElSpace>
```

Required search/table baseline:

```vue
<ArtSearchPanel :collapsible="false" :loading="loading" reset-text="重置" search-text="查询" :show-refresh="false" />
<ArtTableHeader layout="search,size,fullscreen,columns,settings" />
<ArtTable :pagination-options="{ align: 'center', hideOnSinglePage: false, layout: 'sizes, prev, pager, next, jumper', pageSizes: [10, 20, 30], showTotalSummary: true, size: 'small' }" />
```

- [ ] **Step 5: Verify the listener slice**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected:
- The listener API contract passes with CRUD helpers.
- The source contract sees `新增监听器` and the standard list-page shell.
- `@hunyuan/system` typecheck passes.

- [ ] **Step 6: Commit the listener-management slice**

Run:

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/listener.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue
git commit -m "feat: 补齐 BPM 监听器管理页"
```

Expected:
- Git creates one commit containing only the listener management refactor and related BPM contract updates.

## Task 7: Final Closure Verification and Adapter Boundary Audit

**Files:**
- Verify:
  - `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/*.ts`
  - `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/*`
  - `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/**/*.vue`
  - `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`

**Interfaces:**
- Consumes:
  - 全部 BPM 页面、抽屉、设计器适配层、隐藏路由
- Produces:
  - 通过的源码契约、类型校验、适配层边界审计

- [ ] **Step 1: Run the full targeted BPM contract suite**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts --dom
```

Expected:
- All BPM source/API/adapter contract tests PASS.

- [ ] **Step 2: Run the required type checks**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected:
- Both typecheck commands PASS.

- [ ] **Step 3: Audit the “third-party hidden inside adapter” boundary**

Run:

```bash
rg -n "fc-designer|@form-create|bpmn-js|window\\.bpmnInstances" hunyuan-design/apps/hunyuan-system/src/views/system/bpm hunyuan-design/apps/hunyuan-system/src/components/bpm
```

Expected:
- Third-party designer references only appear under `src/components/bpm/adapters/`.
- BPM business pages do not directly import the third-party libraries or touch `window.bpmnInstances`.

- [ ] **Step 4: Audit the hidden-route and drawer semantics**

Run:

```bash
rg -n "/system/bpm/form/designer|/system/bpm/model/designer|useVbenDrawer|ArtEditPage|ArtDetail|ArtSearchPanel|ArtTablePanel" hunyuan-design/apps/hunyuan-system/src/views/system/bpm hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts
```

Expected:
- Hidden designer routes resolve to real local pages.
- `定义 / 实例 / 任务` use `useVbenDrawer`.
- List pages continue to use `ArtSearchPanel + ArtTablePanel + ArtTableHeader + ArtTable`.

- [ ] **Step 5: Commit the final regression-only fixups if and only if verification revealed a direct issue**

Run only when necessary:

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters hunyuan-design/apps/hunyuan-system/src/views/system/bpm hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts
git commit -m "fix: 收口 BPM 管理端回归问题"
```

Expected:
- If verification already passes without edits, do not create this commit.
- If a regression fix is needed, keep it limited to the exact failing file set.

## Self-Review

### Spec Coverage

- `分类`：当前页面已具备完整管理闭环，本计划通过 Task 1 和 Task 7 锁定其仍属于真实本地页面，不另起无意义重构。
- `表单`：Task 4 覆盖元数据列表、设计器入口、真实设计页、保存与预览。
- `模型`：Task 5 覆盖元数据列表、图形设计工作区、保存草稿、校验、模拟、发布。
- `定义 / 实例 / 任务`：Task 3 覆盖统一右侧详情抽屉和 Tab 分面。
- `监听器`：Task 6 覆盖查询、新增、编辑、状态管理。
- `设计器`：Task 2、Task 4、Task 5 覆盖表单设计器适配层、流程设计器适配层、隐藏设计路由。
- “搬能力内核，不搬页面外壳”：Global Constraints、Task 2、Task 7 都对 adapter 边界做了硬约束和审计命令。
- “流程设计继续对齐 simpleModelJson 真相”：Task 2 与 Task 5 已明确桥接 helper 和保存路径。

### Placeholder Scan

- 没有 `TODO`、`TBD`、`implement later`、`similar to Task N` 之类占位词。
- 每个任务都给出了确切文件路径、接口名、命令和预期结果。
- 唯一的前置约束是后端 companion contract；这不是占位，而是显式的集成门槛，已经写入 Global Constraints。

### Type Consistency

- `BpmFormDesignerSnapshot`、`BpmFormDesignerExpose` 只在表单设计器链路使用，名称保持一致。
- `BpmProcessNodeDraft`、`BpmProcessDesignerSnapshot`、`parseSimpleModelDraft`、`stringifySimpleModelDraft` 在 adapter 层和 `model-editor.vue` 中保持同名。
- `getBpmInstanceDetail`、`getBpmTaskDetail`、`queryBpmTaskActionLogs` 的命名在 Task 3 的 API 与抽屉消费方保持一致。
- `buildEmptyBpmFormDesignerSnapshot` 与 `buildEmptyBpmDesignerDraft` 分别对应表单和模型，避免混用。

