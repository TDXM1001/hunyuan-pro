<script setup lang="ts">
import type { ArtActionItem } from '@vben/art-hooks/common';
import type {
  BpmDefinitionCandidateCheck,
  BpmDefinitionDiff,
  BpmDefinitionValidationReport,
  BpmDesignerSaveForm,
} from '#/api/system/bpm';
import type {
  BpmProcessDesignerExpose,
  BpmProcessDesignerSnapshot,
} from '#/components/bpm/adapters/types';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ArtPageActions } from '@vben/art-hooks/common';
import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';
import { Page } from '@vben/common-ui';
import {
  ArrowLeft,
  ArrowUpToLine,
  Check,
  CircleCheckBig,
  RotateCw,
} from '@vben/icons';

import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
} from 'element-plus';

import {
  buildEmptyBpmDesignerDraft,
  getBpmDefinitionPublishDiff,
  getBpmDesignerDetail,
  publishBpmDefinition,
  saveBpmDesignerDraft,
  simulateBpmDesignerDraft,
  validateBpmDefinitionForPublish,
  validateBpmDesignerDraft,
} from '#/api/system/bpm';
import BpmProcessDesignerAdapter from '#/components/bpm/adapters/bpm-process-designer-adapter.vue';
import {
  parseSimpleModelDraft,
  stringifySimpleModelDraft,
} from '#/components/bpm/adapters/simple-model-bridge';

defineOptions({ name: 'SystemBpmModelEditor' });

interface DesignerBaseInfo {
  categoryName: string;
  formName: string;
  formSchemaJson: string;
  hasUnpublishedChanges: boolean;
  instanceNoRuleId?: null | number;
  modelId: number;
  modelKey: string;
  modelName: string;
  publishedDefinitionId?: null | number;
}

const route = useRoute();
const router = useRouter();
const formRef = ref<FormInstance>();
const designerRef = ref<BpmProcessDesignerExpose>();
const designerReady = ref(false);
const loading = ref(false);
const saving = ref(false);
const validating = ref(false);
const simulating = ref(false);
const publishing = ref(false);
const loaded = ref(false);
const activeWorkspace = ref<'design' | 'precheck' | 'rules'>('design');
const savedDraftJson = ref('');
const validationReport = ref<BpmDefinitionValidationReport>();
const publishDiff = ref<BpmDefinitionDiff>();
const candidateChecks = computed<BpmDefinitionCandidateCheck[]>(
  () => validationReport.value?.candidateChecks || [],
);
const blockingCount = computed(
  () => validationReport.value?.blockingCount || 0,
);
const warningCount = computed(() => validationReport.value?.warningCount || 0);
const editorBusy = computed(
  () => loading.value || saving.value || publishing.value,
);

const baseInfo = reactive<DesignerBaseInfo>({
  categoryName: '',
  formName: '',
  formSchemaJson: '',
  hasUnpublishedChanges: false,
  instanceNoRuleId: null,
  modelId: 0,
  modelKey: '',
  modelName: '',
  publishedDefinitionId: null,
});

const formData = reactive<BpmDesignerSaveForm>(buildEmptyBpmDesignerDraft());

const rules: FormRules<BpmDesignerSaveForm> = {
  startRuleJson: [
    { required: true, message: '请输入发起规则 JSON', trigger: 'blur' },
  ],
};

const modelId = computed(() => {
  const rawValue = route.query.modelId;
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
});
const modelPageDescription = computed(() => {
  const details = ['流程模型设计'];
  if (baseInfo.modelKey) {
    details.push(`编码 ${baseInfo.modelKey}`);
  }
  if (baseInfo.categoryName) {
    details.push(baseInfo.categoryName);
  }
  return details.join(' · ');
});

const pageActions = computed<ArtActionItem[]>(() => [
  {
    disabled: editorBusy.value,
    icon: RotateCw,
    key: 'reload',
    label: '重新加载',
    onClick: () => {
      void loadDetail();
    },
  },
  {
    disabled: editorBusy.value,
    icon: Check,
    key: 'save',
    label: '保存草稿',
    loading: saving.value,
    onClick: handleSave,
    type: 'primary',
  },
  {
    disabled: editorBusy.value,
    icon: CircleCheckBig,
    key: 'validate',
    label: '校验',
    loading: validating.value,
    onClick: handleValidate,
  },
  {
    disabled: editorBusy.value,
    icon: ArrowUpToLine,
    key: 'simulate',
    label: '模拟',
    loading: simulating.value,
    onClick: handleSimulate,
  },
  {
    disabled: editorBusy.value,
    key: 'publish',
    label: '发布',
    loading: publishing.value,
    onClick: handlePublish,
    type: 'success',
  },
]);

function resetBaseInfo() {
  Object.assign(baseInfo, {
    categoryName: '',
    formName: '',
    formSchemaJson: '',
    hasUnpublishedChanges: false,
    instanceNoRuleId: null,
    modelId: 0,
    modelKey: '',
    modelName: '',
    publishedDefinitionId: null,
  });
}

function resetFormData() {
  Object.assign(formData, buildEmptyBpmDesignerDraft());
}

function buildDraftJson(draft: BpmDesignerSaveForm) {
  return JSON.stringify({
    managerScopeJson: draft.managerScopeJson,
    simpleModelJson: draft.simpleModelJson,
    startRuleJson: draft.startRuleJson,
    summaryRuleJson: draft.summaryRuleJson,
    titleRuleJson: draft.titleRuleJson,
    variableMappingJson: draft.variableMappingJson,
  });
}

function buildCurrentDraftJson() {
  return buildDraftJson({
    managerScopeJson: formData.managerScopeJson,
    modelId: formData.modelId,
    simpleModelJson: formData.simpleModelJson,
    startRuleJson: formData.startRuleJson,
    summaryRuleJson: formData.summaryRuleJson,
    titleRuleJson: formData.titleRuleJson,
    variableMappingJson: formData.variableMappingJson,
  });
}

function hasUnsavedDraftChanges() {
  return (
    designerRef.value?.isDirty() ||
    savedDraftJson.value !== buildCurrentDraftJson()
  );
}

function isPublishBaselineCurrent(publishBaselineJson: string) {
  return (
    !designerRef.value?.isDirty() &&
    buildCurrentDraftJson() === publishBaselineJson
  );
}

function buildDesignerSnapshot(): BpmProcessDesignerSnapshot {
  try {
    return {
      bpmnXml: '',
      nodes: parseSimpleModelDraft(formData.simpleModelJson || '{"nodes":[]}'),
    };
  } catch {
    return {
      bpmnXml: '',
      nodes: parseSimpleModelDraft('{"nodes":[]}'),
    };
  }
}

async function syncDesignerSnapshot() {
  if (!designerReady.value || !designerRef.value) {
    return;
  }

  await designerRef.value.load(buildDesignerSnapshot());
  designerRef.value.resetDirty();
}

async function loadDetail() {
  if (!modelId.value) {
    resetBaseInfo();
    resetFormData();
    validationReport.value = undefined;
    publishDiff.value = undefined;
    savedDraftJson.value = buildCurrentDraftJson();
    loaded.value = false;
    await syncDesignerSnapshot();
    return;
  }

  loading.value = true;
  try {
    const detail = await getBpmDesignerDetail(modelId.value);
    Object.assign(baseInfo, {
      categoryName: detail.categoryName || '',
      formName: detail.formName || '',
      formSchemaJson: detail.formSchemaJson || '',
      hasUnpublishedChanges: detail.hasUnpublishedChanges ?? false,
      instanceNoRuleId: detail.instanceNoRuleId ?? null,
      modelId: detail.modelId,
      modelKey: detail.modelKey,
      modelName: detail.modelName,
      publishedDefinitionId: detail.publishedDefinitionId ?? null,
    });
    Object.assign(formData, {
      managerScopeJson: detail.managerScopeJson || '',
      modelId: detail.modelId,
      simpleModelJson:
        detail.simpleModelJson || buildEmptyBpmDesignerDraft().simpleModelJson,
      startRuleJson:
        detail.startRuleJson || buildEmptyBpmDesignerDraft().startRuleJson,
      summaryRuleJson: detail.summaryRuleJson || '',
      titleRuleJson: detail.titleRuleJson || '',
      variableMappingJson: detail.variableMappingJson || '',
    });
    savedDraftJson.value = buildCurrentDraftJson();
    loaded.value = true;
    await syncDesignerSnapshot();
    await refreshCandidatePrecheck();
  } finally {
    loading.value = false;
  }
}

function handleDesignerReady() {
  designerReady.value = true;
  void syncDesignerSnapshot();
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    activeWorkspace.value = 'rules';
    return;
  }

  const validateResult = await designerRef.value?.validate();
  if (!validateResult?.ok) {
    activeWorkspace.value = 'design';
    ElMessage.warning(validateResult?.message || '请先修正流程设计');
    return;
  }

  const snapshot = designerRef.value?.getSnapshot();
  if (snapshot) {
    formData.simpleModelJson = stringifySimpleModelDraft(snapshot.nodes);
  }
  const savePayload: BpmDesignerSaveForm = { ...formData };
  const submittedSimpleModelJson = savePayload.simpleModelJson;

  saving.value = true;
  try {
    await saveBpmDesignerDraft(savePayload);
    baseInfo.hasUnpublishedChanges = true;
    savedDraftJson.value = buildDraftJson(savePayload);
    const currentSnapshot = designerRef.value?.getSnapshot();
    const currentSimpleModelJson = currentSnapshot
      ? stringifySimpleModelDraft(currentSnapshot.nodes)
      : formData.simpleModelJson;
    if (currentSimpleModelJson === submittedSimpleModelJson) {
      designerRef.value?.resetDirty();
    }
    await refreshCandidatePrecheck();
    ElMessage.success('流程设计草稿保存成功');
  } finally {
    saving.value = false;
  }
}

async function refreshCandidatePrecheck() {
  if (!formData.modelId) {
    validationReport.value = undefined;
    return;
  }
  validationReport.value = await validateBpmDefinitionForPublish(
    formData.modelId,
  );
}

async function handleValidate() {
  if (!formData.modelId) {
    ElMessage.warning('请先选择有效模型');
    return;
  }

  validating.value = true;
  try {
    const message = await validateBpmDesignerDraft(formData.modelId);
    await refreshCandidatePrecheck();
    const firstBlockingFinding = validationReport.value?.findings[0];
    if (firstBlockingFinding) {
      activeWorkspace.value = 'precheck';
      ElMessage.warning(firstBlockingFinding.message || '流程发布校验未通过');
      return;
    }
    ElMessage.success(message || '设计器校验通过');
  } finally {
    validating.value = false;
  }
}

async function handleSimulate() {
  if (!formData.modelId) {
    ElMessage.warning('请先选择有效模型');
    return;
  }

  simulating.value = true;
  try {
    const message = await simulateBpmDesignerDraft(formData.modelId);
    ElMessage.success(message || '设计器模拟完成');
  } finally {
    simulating.value = false;
  }
}

async function handlePublish() {
  if (!formData.modelId) {
    ElMessage.warning('请先选择有效模型');
    return;
  }
  if (hasUnsavedDraftChanges()) {
    ElMessage.warning('请先保存当前设计后再发布');
    return;
  }
  const publishBaselineJson = buildCurrentDraftJson();

  publishing.value = true;
  try {
    await refreshCandidatePrecheck();
    const report = validationReport.value;
    if (!report?.pass) {
      activeWorkspace.value = 'precheck';
      const firstFinding = report?.findings[0];
      ElMessage.warning(firstFinding?.message || '流程发布校验未通过');
      return;
    }

    publishDiff.value = await getBpmDefinitionPublishDiff(formData.modelId);
    const changedItems =
      publishDiff.value.changedItems.length > 0
        ? publishDiff.value.changedItems.join('、')
        : '无发布差异';
    const confirmed = await ElMessageBox.confirm(
      `确认发布流程定义？${changedItems}`,
      '发布确认',
      {
        cancelButtonText: '取消',
        confirmButtonText: '发布',
        type: 'warning',
      },
    )
      .then(() => true)
      .catch(() => false);
    if (!confirmed) {
      return;
    }
    if (!isPublishBaselineCurrent(publishBaselineJson)) {
      ElMessage.warning('设计已发生变更，请重新发布');
      return;
    }

    await publishBpmDefinition({ modelId: formData.modelId });
    baseInfo.hasUnpublishedChanges = false;
    ElMessage.success('流程定义发布成功');
    await loadDetail();
  } finally {
    publishing.value = false;
  }
}

function handleBack() {
  void router.push('/system/bpm/model');
}

function getCandidateCheckStatusType(
  status: BpmDefinitionCandidateCheck['status'],
) {
  if (status === 'READY') {
    return 'success';
  }
  if (status === 'RUNTIME_REQUIRED') {
    return 'warning';
  }
  return 'danger';
}

function getCandidateCheckStatusLabel(
  status: BpmDefinitionCandidateCheck['status'],
) {
  if (status === 'READY') {
    return '可解析';
  }
  if (status === 'RUNTIME_REQUIRED') {
    return '运行时提供';
  }
  return '阻断';
}

watch(
  modelId,
  () => {
    void loadDetail().catch((error) => {
      ElMessage.error(error?.message || '流程设计器详情加载失败');
    });
  },
  { immediate: true },
);
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtEditPage
      class="model-editor-page"
      :description="modelPageDescription"
      :title="baseInfo.modelName || '流程设计器'"
    >
      <template #back>
        <ElButton :icon="ArrowLeft" link type="primary" @click="handleBack">
          返回模型列表
        </ElButton>
      </template>

      <template #extra>
        <ElTag :type="loaded ? 'success' : 'info'" effect="plain">
          {{ loaded ? '已加载设计器数据' : '等待加载' }}
        </ElTag>
        <ElTag
          :type="baseInfo.hasUnpublishedChanges ? 'warning' : 'success'"
          effect="plain"
        >
          {{
            baseInfo.hasUnpublishedChanges
              ? '存在未发布变更'
              : '与已发布版本一致'
          }}
        </ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <div class="model-editor-page__workspace">
        <ElDescriptions
          class="model-editor-page__summary"
          :column="5"
          size="small"
        >
          <ElDescriptionsItem label="模型编码">
            {{ baseInfo.modelKey || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="流程分类">
            {{ baseInfo.categoryName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="流程表单">
            {{ baseInfo.formName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="单号规则 ID">
            {{ baseInfo.instanceNoRuleId ?? '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="已发布定义 ID">
            {{ baseInfo.publishedDefinitionId ?? '-' }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <ElTabs v-model="activeWorkspace" class="model-editor-page__tabs">
          <ElTabPane label="流程设计" name="design">
            <div class="model-editor-page__designer-pane">
              <BpmProcessDesignerAdapter
                ref="designerRef"
                :disabled="editorBusy"
                :form-schema-json="baseInfo.formSchemaJson"
                :initial-snapshot="buildDesignerSnapshot()"
                :model-key="baseInfo.modelKey"
                :model-name="baseInfo.modelName"
                @ready="handleDesignerReady"
              />
            </div>
          </ElTabPane>

          <ElTabPane label="运行规则" name="rules">
            <div class="model-editor-page__scroll-pane">
              <ElForm
                ref="formRef"
                class="model-editor-form"
                :disabled="editorBusy"
                :model="formData"
                :rules="rules"
                label-position="top"
              >
                <ArtEditSection :columns="1" title="运行规则">
                  <ElFormItem
                    class="art-edit-section__full"
                    label="发起规则 JSON"
                    prop="startRuleJson"
                  >
                    <ElInput
                      v-model="formData.startRuleJson"
                      :rows="6"
                      type="textarea"
                    />
                  </ElFormItem>
                  <ElFormItem
                    class="art-edit-section__full"
                    label="主管范围 JSON"
                    prop="managerScopeJson"
                  >
                    <ElInput
                      v-model="formData.managerScopeJson"
                      :rows="4"
                      type="textarea"
                    />
                  </ElFormItem>
                  <ElFormItem
                    class="art-edit-section__full"
                    label="标题规则 JSON"
                    prop="titleRuleJson"
                  >
                    <ElInput
                      v-model="formData.titleRuleJson"
                      :rows="4"
                      type="textarea"
                    />
                  </ElFormItem>
                  <ElFormItem
                    class="art-edit-section__full"
                    label="摘要规则 JSON"
                    prop="summaryRuleJson"
                  >
                    <ElInput
                      v-model="formData.summaryRuleJson"
                      :rows="4"
                      type="textarea"
                    />
                  </ElFormItem>
                  <ElFormItem
                    class="art-edit-section__full"
                    label="变量映射 JSON"
                    prop="variableMappingJson"
                  >
                    <ElInput
                      v-model="formData.variableMappingJson"
                      :rows="6"
                      type="textarea"
                    />
                  </ElFormItem>
                </ArtEditSection>
              </ElForm>
            </div>
          </ElTabPane>

          <ElTabPane label="发布检查" name="precheck">
            <div class="model-editor-page__scroll-pane">
              <ArtEditSection
                :columns="1"
                description="保存草稿后会刷新候选策略预检结果。"
                title="候选策略预检"
              >
                <div class="candidate-checks-wrap">
                  <div class="candidate-checks-toolbar">
                    <div class="candidate-checks-summary">
                      <ElTag
                        :type="blockingCount > 0 ? 'danger' : 'success'"
                        effect="light"
                      >
                        {{
                          blockingCount > 0 ? `阻断 ${blockingCount}` : '无阻断'
                        }}
                      </ElTag>
                      <ElTag
                        :type="warningCount > 0 ? 'warning' : 'info'"
                        effect="light"
                      >
                        {{
                          warningCount > 0
                            ? `提示 ${warningCount}`
                            : '无额外提示'
                        }}
                      </ElTag>
                    </div>
                    <ElButton
                      link
                      type="primary"
                      @click="refreshCandidatePrecheck"
                    >
                      刷新预检
                    </ElButton>
                  </div>

                  <ElTable
                    :data="candidateChecks"
                    border
                    class="candidate-checks-table"
                    empty-text="保存草稿后可查看候选策略预检结果"
                    size="small"
                  >
                    <ElTableColumn label="节点" min-width="180">
                      <template #default="{ row }">
                        <div class="candidate-check-node">
                          <div class="candidate-check-node__name">
                            {{ row.nodeName || row.nodeKey }}
                          </div>
                          <div class="candidate-check-node__key">
                            {{ row.nodeKey }}
                          </div>
                        </div>
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="策略" min-width="160">
                      <template #default="{ row }">
                        <div class="candidate-check-strategy">
                          <div>
                            {{
                              row.candidateResolverLabel ||
                              row.candidateResolverType
                            }}
                          </div>
                          <div class="candidate-check-node__key">
                            {{ row.candidateResolverType }}
                          </div>
                        </div>
                      </template>
                    </ElTableColumn>
                    <ElTableColumn
                      label="依赖配置"
                      min-width="220"
                      prop="requiredConfig"
                    />
                    <ElTableColumn label="当前可解析" min-width="100">
                      <template #default="{ row }">
                        <ElTag
                          :type="row.canResolveNow ? 'success' : 'info'"
                          effect="light"
                          size="small"
                        >
                          {{ row.canResolveNow ? '是' : '否' }}
                        </ElTag>
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="表单驱动" min-width="100">
                      <template #default="{ row }">
                        <ElTag
                          :type="
                            row.requiresRuntimeFormData ? 'warning' : 'info'
                          "
                          effect="light"
                          size="small"
                        >
                          {{
                            row.requiresRuntimeFormData
                              ? '依赖表单'
                              : '无需表单'
                          }}
                        </ElTag>
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="结果" min-width="280">
                      <template #default="{ row }">
                        <div class="candidate-check-result">
                          <ElTag
                            :type="getCandidateCheckStatusType(row.status)"
                            effect="light"
                            size="small"
                          >
                            {{ getCandidateCheckStatusLabel(row.status) }}
                          </ElTag>
                          <div class="candidate-check-result__message">
                            {{ row.message || '-' }}
                          </div>
                        </div>
                      </template>
                    </ElTableColumn>
                  </ElTable>
                </div>
              </ArtEditSection>
            </div>
          </ElTabPane>
        </ElTabs>
      </div>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.model-editor-page :deep(.art-edit-page__body) {
  overflow: hidden;
}

.model-editor-page__workspace {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.model-editor-page__summary {
  flex: 0 0 auto;
  padding: 12px 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.model-editor-page__summary :deep(.el-descriptions__body) {
  background: transparent;
}

.model-editor-page__tabs {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.model-editor-page__tabs :deep(.el-tabs__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 18px;
}

.model-editor-page__tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--el-border-color-lighter);
}

.model-editor-page__tabs :deep(.el-tabs__content),
.model-editor-page__tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.model-editor-page__tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.model-editor-page__designer-pane {
  height: 100%;
  min-height: 0;
  padding: 12px;
}

.model-editor-page__scroll-pane {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}

.model-editor-form {
  display: grid;
  gap: 12px;
}

.model-editor-form :deep(.el-textarea),
.model-editor-form :deep(.el-select) {
  width: 100%;
}

.candidate-checks-wrap {
  width: 100%;
}

.candidate-checks-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.candidate-checks-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.candidate-checks-table {
  width: 100%;
}

.candidate-check-node,
.candidate-check-strategy,
.candidate-check-result {
  display: grid;
  gap: 4px;
}

.candidate-check-node__name {
  line-height: 1.4;
}

.candidate-check-node__key,
.candidate-check-result__message {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

@media (width <= 1024px) {
  .model-editor-page__summary {
    overflow-x: auto;
  }

  .model-editor-page__summary :deep(.el-descriptions__table) {
    min-width: 760px;
  }
}

@media (width <= 640px) {
  .model-editor-page__tabs :deep(.el-tabs__header) {
    padding: 0 12px;
  }

  .model-editor-page__designer-pane,
  .model-editor-page__scroll-pane {
    padding: 8px;
  }

  .candidate-checks-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
