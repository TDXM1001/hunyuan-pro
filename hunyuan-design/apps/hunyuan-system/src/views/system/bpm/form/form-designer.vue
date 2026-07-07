<script setup lang="ts">
import type { ArtActionItem } from '@vben/art-hooks/common';
import type {
  BpmFormDesignerExpose,
  BpmFormDesignerSnapshot,
} from '#/components/bpm/adapters/types';

import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ArtPageActions } from '@vben/art-hooks/common';
import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  buildBpmFormDesignerPayload,
  buildEmptyBpmFormDesignerSnapshot,
  getBpmFormDetail,
  updateBpmForm,
} from '#/api/system/bpm';
import BpmFormDesignerAdapter from '#/components/bpm/adapters/bpm-form-designer-adapter.vue';

defineOptions({ name: 'SystemBpmFormDesigner' });

interface BpmFormDesignerPageModel {
  disabledFlag: boolean;
  formId: number;
  formKey: string;
  formName: string;
  layoutJson: string;
  remark: string;
  schemaJson: string;
}

const route = useRoute();
const router = useRouter();
const designerRef = ref<BpmFormDesignerExpose>();
const designerReady = ref(false);
const loading = ref(false);
const saving = ref(false);
const previewVisible = ref(false);
const previewSnapshot = ref<BpmFormDesignerSnapshot>(
  buildEmptyBpmFormDesignerSnapshot(),
);

const formData = reactive<BpmFormDesignerPageModel>({
  disabledFlag: false,
  formId: 0,
  formKey: '',
  formName: '',
  ...buildEmptyBpmFormDesignerSnapshot(),
  remark: '',
});

const formId = computed(() => {
  const rawValue = route.query.formId;
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
});

const pageActions = computed<ArtActionItem[]>(() => [
  {
    key: 'reload',
    label: '重新加载',
    loading: loading.value,
    onClick: () => {
      void loadDetail();
    },
  },
  {
    key: 'preview',
    label: '预览快照',
    onClick: handlePreview,
  },
  {
    key: 'save',
    label: '保存设计',
    loading: saving.value,
    onClick: handleSave,
    type: 'primary',
  },
]);

function resetFormData() {
  Object.assign(formData, {
    disabledFlag: false,
    formId: 0,
    formKey: '',
    formName: '',
    ...buildEmptyBpmFormDesignerSnapshot(),
    remark: '',
  });
}

function getDesignerSnapshot(): BpmFormDesignerSnapshot {
  return {
    layoutJson: formData.layoutJson || '{}',
    schemaJson: formData.schemaJson || '[]',
  };
}

async function syncDesignerSnapshot() {
  if (!designerReady.value || !designerRef.value) {
    return;
  }

  await designerRef.value.load(getDesignerSnapshot());
  designerRef.value.resetDirty();
}

async function loadDetail() {
  if (!formId.value) {
    resetFormData();
    await syncDesignerSnapshot();
    return;
  }

  loading.value = true;
  try {
    const detail = await getBpmFormDetail(formId.value);
    Object.assign(formData, {
      disabledFlag: detail.disabledFlag ?? false,
      formId: detail.formId,
      formKey: detail.formKey,
      formName: detail.formName,
      layoutJson: detail.layoutJson || '{}',
      remark: detail.remark || '',
      schemaJson: detail.schemaJson || '[]',
    });
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
  if (!formData.formId) {
    ElMessage.warning('请先在表单列表新增表单基础信息');
    return;
  }

  const validateResult = await designerRef.value?.validate();
  if (!validateResult?.ok) {
    ElMessage.warning(validateResult?.message || '请先修正表单设计');
    return;
  }

  const snapshot = designerRef.value?.getSnapshot();
  if (!snapshot) {
    return;
  }

  saving.value = true;
  try {
    const designerPayload = buildBpmFormDesignerPayload(snapshot);
    await updateBpmForm({
      disabledFlag: formData.disabledFlag,
      formId: formData.formId,
      formKey: formData.formKey,
      formName: formData.formName,
      remark: formData.remark || '',
      ...designerPayload,
    });
    Object.assign(formData, designerPayload);
    designerRef.value?.resetDirty();
    ElMessage.success('流程表单设计保存成功');
  } finally {
    saving.value = false;
  }
}

function handlePreview() {
  previewSnapshot.value = designerRef.value?.getSnapshot() || getDesignerSnapshot();
  previewVisible.value = true;
}

function handleBack() {
  void router.push('/system/bpm/form');
}

watch(
  formId,
  () => {
    void loadDetail().catch((error) => {
      ElMessage.error(error?.message || '流程表单设计数据加载失败');
    });
  },
  { immediate: true },
);
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtEditPage title="表单设计器">
      <template #back>
        <ElButton link type="primary" @click="handleBack">返回表单列表</ElButton>
      </template>

      <template #extra>
        <ElTag effect="light" round type="info">
          {{ formId ? `表单 ID：${formId}` : '未绑定表单' }}
        </ElTag>
        <ElTag :type="formData.disabledFlag ? 'danger' : 'success'" effect="light" round>
          {{ formData.disabledFlag ? '已禁用' : '已启用' }}
        </ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <ElForm
        :model="formData"
        class="form-designer-page__form"
        :disabled="loading"
        label-position="top"
      >
        <ArtEditSection title="表单信息" :index="1">
          <ElFormItem label="表单编码">
            <ElInput :model-value="formData.formKey" disabled />
          </ElFormItem>
          <ElFormItem label="表单名称">
            <ElInput :model-value="formData.formName" disabled />
          </ElFormItem>
          <ElFormItem label="禁用状态">
            <ElSwitch :model-value="formData.disabledFlag" disabled />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="备注">
            <ElInput :model-value="formData.remark" disabled type="textarea" />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="设计工作区" :index="2">
          <div class="form-designer-page__workbench">
            <BpmFormDesignerAdapter
              ref="designerRef"
              :disabled="loading"
              :initial-snapshot="getDesignerSnapshot()"
              @ready="handleDesignerReady"
            />
          </div>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>

    <ElDialog v-model="previewVisible" title="表单设计快照预览" width="880px">
      <div class="form-designer-page__preview">
        <div class="form-designer-page__preview-block">
          <div class="form-designer-page__preview-title">Schema JSON</div>
          <ElInput
            :model-value="previewSnapshot.schemaJson"
            :rows="12"
            readonly
            type="textarea"
          />
        </div>

        <div class="form-designer-page__preview-block">
          <div class="form-designer-page__preview-title">布局 JSON</div>
          <ElInput
            :model-value="previewSnapshot.layoutJson"
            :rows="10"
            readonly
            type="textarea"
          />
        </div>
      </div>
    </ElDialog>
  </Page>
</template>

<style scoped>
.form-designer-page__form {
  display: grid;
  gap: 12px;
}

.form-designer-page__workbench {
  min-height: 560px;
  overflow: hidden;
}

.form-designer-page__preview {
  display: grid;
  gap: 16px;
}

.form-designer-page__preview-block {
  display: grid;
  gap: 8px;
}

.form-designer-page__preview-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}
</style>
