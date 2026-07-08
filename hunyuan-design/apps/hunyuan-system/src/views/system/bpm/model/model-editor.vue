<script setup lang="ts">
import type { ArtActionItem } from '@vben/art-hooks/common';
import type {
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
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
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
const validationReport = ref<BpmDefinitionValidationReport>();
const publishDiff = ref<BpmDefinitionDiff>();

const baseInfo = reactive<DesignerBaseInfo>({
  categoryName: '',
  formName: '',
  hasUnpublishedChanges: false,
  instanceNoRuleId: null,
  modelId: 0,
  modelKey: '',
  modelName: '',
  publishedDefinitionId: null,
});

const formData = reactive<BpmDesignerSaveForm>(buildEmptyBpmDesignerDraft());

const rules: FormRules<BpmDesignerSaveForm> = {
  startRuleJson: [{ required: true, message: '请输入发起规则 JSON', trigger: 'blur' }],
};

const modelId = computed(() => {
  const rawValue = route.query.modelId;
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
});

const pageActions = computed<ArtActionItem[]>(() => [
  {
    key: 'reload',
    label: '重新加载',
    onClick: () => {
      void loadDetail();
    },
  },
  {
    key: 'save',
    label: '保存草稿',
    loading: saving.value,
    onClick: handleSave,
    type: 'primary',
  },
  {
    key: 'validate',
    label: '校验',
    loading: validating.value,
    onClick: handleValidate,
  },
  {
    key: 'simulate',
    label: '模拟',
    loading: simulating.value,
    onClick: handleSimulate,
  },
  {
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
      startRuleJson: detail.startRuleJson || buildEmptyBpmDesignerDraft().startRuleJson,
      summaryRuleJson: detail.summaryRuleJson || '',
      titleRuleJson: detail.titleRuleJson || '',
      variableMappingJson: detail.variableMappingJson || '',
    });
    loaded.value = true;
    await syncDesignerSnapshot();
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
    return;
  }

  const validateResult = await designerRef.value?.validate();
  if (!validateResult?.ok) {
    ElMessage.warning(validateResult?.message || '请先修正流程设计');
    return;
  }

  const snapshot = designerRef.value?.getSnapshot();
  if (snapshot) {
    formData.simpleModelJson = stringifySimpleModelDraft(snapshot.nodes);
  }

  saving.value = true;
  try {
    await saveBpmDesignerDraft(formData);
    baseInfo.hasUnpublishedChanges = true;
    designerRef.value?.resetDirty();
    ElMessage.success('流程设计草稿保存成功');
  } finally {
    saving.value = false;
  }
}

async function handleValidate() {
  if (!formData.modelId) {
    ElMessage.warning('请先选择有效模型');
    return;
  }

  validating.value = true;
  try {
    const message = await validateBpmDesignerDraft(formData.modelId);
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

  publishing.value = true;
  try {
    validationReport.value = await validateBpmDefinitionForPublish(formData.modelId);
    if (!validationReport.value.pass) {
      const firstFinding = validationReport.value.findings[0];
      ElMessage.warning(firstFinding?.message || '流程发布校验未通过');
      return;
    }

    publishDiff.value = await getBpmDefinitionPublishDiff(formData.modelId);
    const changedItems = publishDiff.value.changedItems.length > 0
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
    <ArtEditPage title="流程设计器">
      <template #back>
        <ElButton link type="primary" @click="handleBack">返回模型列表</ElButton>
      </template>

      <template #extra>
        <ElTag :type="loaded ? 'success' : 'info'" effect="light" round>
          {{ loaded ? '已加载设计器数据' : '等待加载' }}
        </ElTag>
        <ElTag
          :type="baseInfo.hasUnpublishedChanges ? 'warning' : 'success'"
          effect="light"
          round
        >
          {{ baseInfo.hasUnpublishedChanges ? '存在未发布变更' : '与已发布版本一致' }}
        </ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <ElForm
        ref="formRef"
        class="model-editor-form"
        :disabled="loading"
        :model="formData"
        :rules="rules"
        label-position="top"
      >
        <ArtEditSection title="模型信息" :index="1">
          <ElFormItem label="模型编码">
            <ElInput :model-value="baseInfo.modelKey" disabled />
          </ElFormItem>
          <ElFormItem label="模型名称">
            <ElInput :model-value="baseInfo.modelName" disabled />
          </ElFormItem>
          <ElFormItem label="流程分类">
            <ElInput :model-value="baseInfo.categoryName" disabled />
          </ElFormItem>
          <ElFormItem label="流程表单">
            <ElInput :model-value="baseInfo.formName" disabled />
          </ElFormItem>
          <ElFormItem label="单号规则 ID">
            <ElInputNumber :model-value="baseInfo.instanceNoRuleId ?? undefined" disabled />
          </ElFormItem>
          <ElFormItem label="已发布定义 ID">
            <ElInputNumber :model-value="baseInfo.publishedDefinitionId ?? undefined" disabled />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="流程设计工作区" :index="2">
          <BpmProcessDesignerAdapter
            ref="designerRef"
            :disabled="loading"
            :initial-snapshot="buildDesignerSnapshot()"
            :model-key="baseInfo.modelKey"
            :model-name="baseInfo.modelName"
            @ready="handleDesignerReady"
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
      </ElForm>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.model-editor-form {
  display: grid;
  gap: 12px;
}

.model-editor-form :deep(.el-input-number),
.model-editor-form :deep(.el-textarea),
.model-editor-form :deep(.el-select) {
  width: 100%;
}
</style>
