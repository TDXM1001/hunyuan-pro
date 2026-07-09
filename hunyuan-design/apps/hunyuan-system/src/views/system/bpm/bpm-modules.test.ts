import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const categoryPagePath =
  'apps/hunyuan-system/src/views/system/bpm/category/category-list.vue';
const formListPagePath =
  'apps/hunyuan-system/src/views/system/bpm/form/form-list.vue';
const formDesignerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue';
const modelListPagePath =
  'apps/hunyuan-system/src/views/system/bpm/model/model-list.vue';
const modelEditorPath =
  'apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue';
const definitionPagePath =
  'apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue';
const instancePagePath =
  'apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue';
const taskPagePath =
  'apps/hunyuan-system/src/views/system/bpm/task/task-list.vue';
const listenerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue';
const callbackRecordPagePath =
  'apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue';
const commandRecordPagePath =
  'apps/hunyuan-system/src/views/system/bpm/integration/command-record-list.vue';
const runtimeStartablePath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue';
const runtimeStartFormPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/start-form.vue';
const runtimeMyInstancePath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-instance-list.vue';
const runtimeMyTodoPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue';
const runtimeMyDonePath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-done-list.vue';
const runtimeMyCopyPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-copy-list.vue';
const runtimeFormRendererPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue';
const runtimeDetailDrawerPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue';
const bpmAdminMenuSqlPath = '数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql';
const bpmRuntimeMenuSqlPath = '数据库SQL脚本/mysql/sql-update-log/v3.35.0.sql';
const bpmRuntimeCopyMenuSqlPath =
  '数据库SQL脚本/mysql/sql-update-log/v3.37.0.sql';
const bpmIntegrationMenuSqlPath =
  '数据库SQL脚本/mysql/sql-update-log/v3.39.0.sql';
const apiPath = 'apps/hunyuan-system/src/api/system/bpm/index.ts';
const bpmRoutePath = 'apps/hunyuan-system/src/router/routes/static/bpm.ts';
const runtimeApiPath = 'apps/hunyuan-system/src/api/system/bpm/runtime.ts';
const bootstrapPath = 'apps/hunyuan-system/src/bootstrap.ts';

function readSource(path: string) {
  return readFileSync(resolve(process.cwd(), path), 'utf8');
}

describe('bpm module contracts', () => {
  it('provides real bpm management pages instead of bridge placeholders', () => {
    [
      categoryPagePath,
      formDesignerPagePath,
      modelEditorPath,
      definitionPagePath,
      instancePagePath,
      taskPagePath,
      listenerPagePath,
      callbackRecordPagePath,
      commandRecordPagePath,
    ].forEach((path) => {
      expect(existsSync(resolve(process.cwd(), path))).toBe(true);
    });
  });

  it('keeps the category page on the compact table-page pattern', () => {
    const categorySource = readSource(categoryPagePath);

    expect(categorySource).toContain('ArtSearchPanel');
    expect(categorySource).toContain('ArtTablePanel');
    expect(categorySource).toContain(':collapsible="false"');
    expect(categorySource).not.toContain('category-page__title');
    expect(categorySource).not.toContain('module-bridge');
  });

  it('keeps hidden local routes for form and model designers', () => {
    const routeSource = readSource(bpmRoutePath);

    expect(routeSource).toContain('/system/bpm/form/designer');
    expect(routeSource).toContain('/system/bpm/model/designer');
    expect(routeSource).toContain('hideInMenu: true');
  });

  it('keeps bpm management list pages aligned to shared table primitives', () => {
    [
      categoryPagePath,
      definitionPagePath,
      instancePagePath,
      taskPagePath,
      listenerPagePath,
      callbackRecordPagePath,
      commandRecordPagePath,
    ].forEach((path) => {
      const source = readSource(path);

      expect(source).toContain('ArtSearchPanel');
      expect(source).toContain('ArtTablePanel');
      expect(source).toContain('ArtTableHeader');
      expect(source).toContain('ArtTable');
    });
  });

  it('keeps the form designer on the shared edit-page baseline', () => {
    const formDesignerSource = readSource(formDesignerPagePath);

    expect(formDesignerSource).toContain('ArtEditPage');
    expect(formDesignerSource).toContain('ArtEditSection');
    expect(formDesignerSource).toContain('SystemBpmFormDesigner');
    expect(formDesignerSource).toContain('BpmFormDesignerAdapter');
  });

  it('keeps the form list focused on metadata plus a designer entry', () => {
    const formListSource = readSource(formListPagePath);

    expect(formListSource).toContain('openDesigner');
    expect(formListSource).toContain('/system/bpm/form/designer');
  });

  it('keeps the model editor on the shared edit baseline and process designer adapter', () => {
    const modelEditorSource = readSource(modelEditorPath);

    expect(modelEditorSource).toContain('ArtEditPage');
    expect(modelEditorSource).toContain('ArtEditSection');
    expect(modelEditorSource).toContain('BpmProcessDesignerAdapter');
    expect(modelEditorSource).toContain('validateBpmDefinitionForPublish');
    expect(modelEditorSource).toContain('getBpmDefinitionPublishDiff');
    expect(modelEditorSource).toContain('validationReport');
    expect(modelEditorSource).toContain('publishDiff');
    expect(modelEditorSource).not.toContain('simpleModel JSON');
  });

  it('keeps the definition list wired to P1.1 governance actions', () => {
    const definitionSource = readSource(definitionPagePath);

    expect(definitionSource).toContain('suspendBpmDefinitionStart');
    expect(definitionSource).toContain('enableBpmDefinitionStart');
    expect(definitionSource).toContain('handleSuspendStart');
    expect(definitionSource).toContain('handleEnableStart');
  });

  it('keeps the model list wired to the hidden designer route', () => {
    const modelListSource = readSource(modelListPagePath);

    expect(modelListSource).toContain('openDesigner');
    expect(modelListSource).toContain('/system/bpm/model/designer');
  });

  it('keeps the bpm api barrel aligned to real backend module contracts', () => {
    const source = readSource(apiPath);

    expect(source).toContain('/bpm/category/');
    expect(source).toContain('/bpm/form/');
    expect(source).toContain('/bpm/model/');
    expect(source).toContain('/bpm/definition/');
    expect(source).toContain('/bpm/designer/');
    expect(source).toContain('/bpm/task/');
    expect(source).toContain('/bpm/listener/');
    expect(source).toContain('/bpm/integration/');
  });

  it('keeps bpm integration monitoring pages wired to reliability APIs', () => {
    const callbackSource = readSource(callbackRecordPagePath);
    const commandSource = readSource(commandRecordPagePath);

    expect(callbackSource).toContain('queryBpmCallbackRecordPage');
    expect(callbackSource).toContain('retryBpmCallbackRecord');
    expect(callbackSource).toContain('compensateBpmCallbackRecord');
    expect(callbackSource).toContain(':collapsible="false"');
    expect(callbackSource).toContain('callbackStatus === 2');
    expect(callbackSource).toContain('callbackStatus === 3');
    expect(callbackSource).toContain('需人工补偿');
    expect(callbackSource).toContain('nextRetryAt');
    expect(commandSource).toContain('queryBpmCommandRecordPage');
    expect(commandSource).toContain(':collapsible="false"');
  });

  it('provides runtime pages and runtime api bindings for the employee-side flow', () => {
    [
      runtimeStartablePath,
      runtimeStartFormPath,
      runtimeMyInstancePath,
      runtimeMyTodoPath,
      runtimeMyDonePath,
      runtimeMyCopyPath,
      runtimeDetailDrawerPath,
    ].forEach((path) => {
      expect(existsSync(resolve(process.cwd(), path))).toBe(true);
    });

    const runtimeApiSource = readSource(runtimeApiPath);

    expect(runtimeApiSource).toContain('/app/bpm/startable');
    expect(runtimeApiSource).toContain('/app/bpm/start-draft/');
    expect(runtimeApiSource).toContain('/app/bpm/start');
    expect(runtimeApiSource).toContain('/app/bpm/my-instance');
    expect(runtimeApiSource).toContain('/app/bpm/resubmit-draft/');
    expect(runtimeApiSource).toContain('/app/bpm/instance/cancel');
    expect(runtimeApiSource).toContain('/app/bpm/instance/resubmit');
    expect(runtimeApiSource).toContain('/app/bpm/my-todo');
    expect(runtimeApiSource).toContain('/app/bpm/my-done');
    expect(runtimeApiSource).toContain('/app/bpm/my-copy');
    expect(runtimeApiSource).toContain('/app/bpm/copy/read/');
    expect(runtimeApiSource).toContain('/app/bpm/instance/detail/');
    expect(runtimeApiSource).toContain('currentTasks?: BpmTaskRecord[]');
  });

  it('keeps my-instance resubmit entry routed through the unified start form', () => {
    const startableSource = readSource(runtimeStartablePath);
    const myInstanceSource = readSource(runtimeMyInstancePath);
    const detailSource = readSource(runtimeDetailDrawerPath);

    expect(startableSource).toContain('SystemBpmRuntimeStartFormRoute');
    expect(myInstanceSource).toContain('handleResubmit');
    expect(myInstanceSource).toContain("name: 'SystemBpmRuntimeStartFormRoute'");
    expect(myInstanceSource).toContain('instanceId: String(row.instanceId)');
    expect(detailSource).toContain('getCallbackStatusLabel');
    expect(detailSource).toContain('nextRetryAt');
    expect(detailSource).toContain('需人工补偿');
    expect(detailSource).toContain('INSTANCE_CANCELLED');
    expect(detailSource).toContain('RESUBMITTED');
  });

  it('keeps the unified start form guarded until draft loading succeeds', () => {
    const startFormSource = readSource(runtimeStartFormPath);

    expect(startFormSource).toContain(
      'disabled: loading.value || submitting.value || !loaded.value',
    );
    expect(startFormSource).toContain('if (!loaded.value) {');
  });

  it('keeps the unified start form in an explicit error state on draft-load failure', () => {
    const startFormSource = readSource(runtimeStartFormPath);

    expect(startFormSource).toContain("const loadErrorMessage = ref('');");
    expect(startFormSource).toContain('loadErrorMessage.value =');
    expect(startFormSource).toContain('v-if="loadErrorMessage"');
  });

  it('keeps the runtime form renderer compatible with draft schemas wrapped in a fields object', () => {
    const rendererSource = readSource(runtimeFormRendererPath);

    expect(rendererSource).toContain('const parsed = safeParseJson<unknown>(props.schemaJson, []);');
    expect(rendererSource).toContain("if (parsed && typeof parsed === 'object' && Array.isArray((parsed as { fields?: unknown }).fields)) {");
    expect(rendererSource).toContain('return (parsed as { fields: FormRule[] }).fields;');
  });

  it('keeps runtime bootstrap wiring form-create registration before the runtime form page uses it', () => {
    const bootstrapSource = readSource(bootstrapPath);

    expect(bootstrapSource).toContain("import formCreate from '@form-create/element-ui';");
    expect(bootstrapSource).toContain("import installFormCreateAutoImport from '@form-create/element-ui/auto-import';");
    expect(bootstrapSource).toContain('installFormCreateAutoImport(formCreate as any);');
    expect(bootstrapSource).toContain('app.use(formCreate as any);');
  });

  it('keeps the runtime detail drawer in an explicit error state on detail-load failure', () => {
    const detailSource = readSource(runtimeDetailDrawerPath);

    expect(detailSource).toContain("const loadErrorMessage = ref('');");
    expect(detailSource).toContain('detail.value = undefined;');
    expect(detailSource).toContain('loadErrorMessage.value =');
    expect(detailSource).toContain('v-else-if="loadErrorMessage"');
    expect(detailSource).toContain('currentTasks');
    expect(detailSource).toContain('当前待办');
    expect(detailSource).toContain('暂无当前待办');
    expect(detailSource).toContain('fromAssigneeEmployeeId');
    expect(detailSource).toContain('toAssigneeEmployeeId');
    expect(detailSource).toContain('getBpmAdminInstanceDetail');
    expect(detailSource).toContain('getBpmAdminInstanceTrace');
    expect(detailSource).toContain("source === 'admin'");
    expect(detailSource).toContain('trace.value = undefined;');
    expect(detailSource).toContain('可靠性追踪');
    expect(detailSource).toContain('callbackRecords');
    expect(detailSource).toContain('commandRecords');
    expect(detailSource).toContain('notificationRecords');
    expect(detailSource).toContain('通知记录');
  });

  it('keeps the admin instance page aligned to four backend run states', () => {
    const instanceSource = readSource(instancePagePath);

    expect(instanceSource).toContain('function getRunStateLabel');
    expect(instanceSource).toContain('if (value === 2) {');
    expect(instanceSource).toContain('if (value === 3) {');
    expect(instanceSource).toContain('if (value === 4) {');
    expect(instanceSource).toContain(':value="2"');
    expect(instanceSource).toContain(':value="3"');
    expect(instanceSource).toContain(':value="4"');
  });

  it('keeps the admin instance page wired to the unified bpm instance detail drawer', () => {
    const instanceSource = readSource(instancePagePath);

    expect(instanceSource).toContain('BpmInstanceDetailDrawer');
    expect(instanceSource).toContain('detailDrawerRef');
    expect(instanceSource).toContain("detailDrawerRef.value?.open(row.instanceId, 'admin')");
    expect(instanceSource).not.toContain('ElDialog v-model="detailVisible"');
    expect(instanceSource).not.toContain('getBpmAdminInstanceDetail');
  });

  it('keeps the admin task page wired to a local detail dialog', () => {
    const taskSource = readSource(taskPagePath);

    expect(taskSource).toContain('getBpmTaskDetail');
    expect(taskSource).toContain('openDetailDialog');
    expect(taskSource).toContain('detailVisible');
    expect(taskSource).toContain('ElDialog');
    expect(taskSource).toContain('detailLoadErrorMessage');
  });

  it('keeps the runtime todo page wired to a local task-detail dialog before action handling', () => {
    const todoSource = readSource(runtimeMyTodoPath);

    expect(todoSource).toContain('getBpmTaskDetail');
    expect(todoSource).toContain('openDetailDialog');
    expect(todoSource).toContain('detailVisible');
    expect(todoSource).toContain('ElDialog');
    expect(todoSource).toContain('detailLoadErrorMessage');
    expect(todoSource).toContain('handleApprove');
    expect(todoSource).toContain('handleReturn');
  });

  it('keeps the runtime todo approval actions wired to optional manual copy employees', () => {
    const todoSource = readSource(runtimeMyTodoPath);

    expect(todoSource).toContain('queryEmployeePage');
    expect(todoSource).toContain('copyEmployeeIds');
    expect(todoSource).toContain('openActionDialog');
    expect(todoSource).toContain('submitActionDialog');
    expect(todoSource).not.toContain("ElMessageBox.prompt('请输入审批意见'");
  });

  it('keeps the runtime copy page wired to my-copy api and unified instance detail drawer', () => {
    const copySource = readSource(runtimeMyCopyPath);

    expect(copySource).toContain("defineOptions({ name: 'SystemBpmRuntimeMyCopyList' })");
    expect(copySource).toContain('ArtSearchPanel');
    expect(copySource).toContain('ArtTablePanel');
    expect(copySource).toContain(':collapsible="false"');
    expect(copySource).toContain('BpmInstanceDetailDrawer');
    expect(copySource).toContain('queryMyBpmCopyPage');
    expect(copySource).toContain('markBpmCopyRead');
    expect(copySource).toContain('detailDrawerRef.value?.open(row.instanceId)');
  });

  it('keeps the admin instance route aligned to the backend menu sql contract', () => {
    const bpmMenuSqlSource = readFileSync(
      resolve(process.cwd(), '..', bpmAdminMenuSqlPath),
      'utf8',
    );

    expect(bpmMenuSqlSource).toContain("'/system/bpm/instance'");
    expect(bpmMenuSqlSource).toContain("'/system/bpm/instance/instance-list.vue'");
  });

  it('keeps the runtime bpm menu sql aligned to the employee route contracts', () => {
    const bpmMenuSqlSource = readFileSync(
      resolve(process.cwd(), '..', bpmRuntimeMenuSqlPath),
      'utf8',
    );

    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/startable-list'");
    expect(bpmMenuSqlSource).toContain(
      "'/system/bpm/runtime/startable-list.vue'",
    );
    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-instance-list'");
    expect(bpmMenuSqlSource).toContain(
      "'/system/bpm/runtime/my-instance-list.vue'",
    );
    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-todo-list'");
    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-todo-list.vue'");
    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-done-list'");
    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-done-list.vue'");
  });

  it('keeps the runtime copy menu sql aligned to the employee route contract', () => {
    const bpmMenuSqlSource = readFileSync(
      resolve(process.cwd(), '..', bpmRuntimeCopyMenuSqlPath),
      'utf8',
    );

    expect(bpmMenuSqlSource).toContain("'我的抄送'");
    expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-copy-list'");
    expect(bpmMenuSqlSource).toContain(
      "'/system/bpm/runtime/my-copy-list.vue'",
    );
    expect(bpmMenuSqlSource).toContain("'bpm_runtime_user'");
    expect(bpmMenuSqlSource).toContain('320');
  });

  it('keeps the bpm integration monitor menu sql aligned to page and permission contracts', () => {
    const bpmMenuSqlSource = readFileSync(
      resolve(process.cwd(), '..', bpmIntegrationMenuSqlPath),
      'utf8',
    );

    expect(bpmMenuSqlSource).toContain("'/system/bpm/integration/callback-record-list'");
    expect(bpmMenuSqlSource).toContain(
      "'/system/bpm/integration/callback-record-list.vue'",
    );
    expect(bpmMenuSqlSource).toContain("'/system/bpm/integration/command-record-list'");
    expect(bpmMenuSqlSource).toContain(
      "'/system/bpm/integration/command-record-list.vue'",
    );
    expect(bpmMenuSqlSource).toContain("'bpm:integration:query'");
    expect(bpmMenuSqlSource).toContain("'bpm:integration:update'");
    expect(bpmMenuSqlSource).toContain('321');
    expect(bpmMenuSqlSource).toContain('325');
  });

  it('provides local static routes for runtime bpm pages', () => {
    const routeSource = readSource(bpmRoutePath);

    expect(routeSource).toContain("import { BasicLayout } from '#/layouts';");
    expect(routeSource).toContain('SystemBpmRuntimeShellRoute');
    expect(routeSource).toContain("path: '/system/bpm/runtime'");
    expect(routeSource).toContain('/system/bpm/runtime/start-form');
    expect(routeSource).toContain('hideInMenu: true');
    expect(routeSource).not.toContain('/system/bpm/runtime/startable-list');
    expect(routeSource).not.toContain('/system/bpm/runtime/my-instance-list');
    expect(routeSource).not.toContain('/system/bpm/runtime/my-todo-list');
    expect(routeSource).not.toContain('/system/bpm/runtime/my-done-list');
  });
});
