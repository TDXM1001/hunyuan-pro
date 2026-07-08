<script setup lang="ts">
import type { ArtActionItem } from '@vben/art-hooks/common';
import type {
  BpmInstanceResubmitForm,
  BpmInstanceStartForm,
  BpmRuntimeStartDraftRecord,
} from '#/api/system/bpm';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ArtPageActions } from '@vben/art-hooks/common';
import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElTag,
} from 'element-plus';

import {
  getBpmResubmitDraft,
  getBpmStartDraft,
  resubmitMyBpmInstance,
  startBpmInstance,
} from '#/api/system/bpm';

import BpmRuntimeFormRenderer from './components/bpm-runtime-form-renderer.vue';

defineOptions({ name: 'SystemBpmRuntimeStartForm' });

interface RuntimeStartFormState {
  definitionId: number;
  definitionName: string;
  formNameSnapshot: string;
  formSchemaSnapshotJson: string;
  sourceInstanceId?: null | number;
  summary: string;
  title: string;
}

interface BpmRuntimeFormRendererExpose {
  submit: () => Promise<Record<string, any>>;
}

const route = useRoute();
const router = useRouter();
const formRef = ref<FormInstance>();
const runtimeFormRef = ref<BpmRuntimeFormRendererExpose>();
const loading = ref(false);
const loaded = ref(false);
const loadErrorMessage = ref('');
const submitting = ref(false);
const runtimeFormData = ref<Record<string, any>>({});

const formState = reactive<RuntimeStartFormState>({
  definitionId: 0,
  definitionName: '',
  formNameSnapshot: '',
  formSchemaSnapshotJson: '[]',
  sourceInstanceId: null,
  summary: '',
  title: '',
});

const formRules: FormRules<RuntimeStartFormState> = {
  title: [{ required: true, message: '请输入流程标题', trigger: 'blur' }],
};

const routeDefinitionId = computed(() => parseQueryId(route.query.definitionId));
const routeInstanceId = computed(() => parseQueryId(route.query.instanceId));
const isResubmitMode = computed(() => routeInstanceId.value > 0);
const pageTitle = computed(() => (isResubmitMode.value ? '重新提交流程' : '发起流程'));
const backLabel = computed(() => (isResubmitMode.value ? '返回我的申请' : '返回可发起流程'));
const submitLabel = computed(() => (isResubmitMode.value ? '重新提交' : '发起流程'));
const runtimeFormKey = computed(
  () =>
    `${formState.definitionId}-${formState.sourceInstanceId ?? 0}-${isResubmitMode.value ? 'resubmit' : 'start'}`,
);

const pageActions = computed<ArtActionItem[]>(() => [
  {
    key: 'reload',
    label: '重新加载',
    onClick: () => {
      void loadDraft();
    },
  },
  {
    disabled: loading.value || submitting.value || !loaded.value,
    key: 'submit',
    label: submitLabel.value,
    loading: submitting.value,
    onClick: handleSubmit,
    type: 'primary',
  },
]);

function parseQueryId(rawValue: unknown) {
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

function cloneJson<T>(value: T): T {
  if (value === null || value === undefined) {
    return {} as T;
  }
  return JSON.parse(JSON.stringify(value)) as T;
}

function safeParseJson<T>(jsonText: string | undefined, fallbackValue: T): T {
  if (!jsonText?.trim()) {
    return fallbackValue;
  }

  try {
    return JSON.parse(jsonText) as T;
  } catch {
    return fallbackValue;
  }
}

function resetFormState() {
  Object.assign(formState, {
    definitionId: 0,
    definitionName: '',
    formNameSnapshot: '',
    formSchemaSnapshotJson: '[]',
    sourceInstanceId: null,
    summary: '',
    title: '',
  });
  loadErrorMessage.value = '';
  runtimeFormData.value = {};
  loaded.value = false;
}

function applyDraft(draft: BpmRuntimeStartDraftRecord) {
  Object.assign(formState, {
    definitionId: draft.definitionId,
    definitionName: draft.definitionName,
    formNameSnapshot: draft.formNameSnapshot || '',
    formSchemaSnapshotJson: draft.formSchemaSnapshotJson || '[]',
    sourceInstanceId: draft.sourceInstanceId ?? null,
    summary: draft.summary || '',
    title: draft.title || draft.definitionName,
  });
  runtimeFormData.value = safeParseJson<Record<string, any>>(draft.formDataJson, {});
  loaded.value = true;
}

async function loadDraft() {
  const definitionId = routeDefinitionId.value;
  const instanceId = routeInstanceId.value;

  if (!definitionId && !instanceId) {
    ElMessage.warning('缺少有效的流程定义或实例参数');
    handleBack();
    return;
  }

  loading.value = true;
  resetFormState();
  try {
    const draft = instanceId
      ? await getBpmResubmitDraft(instanceId)
      : await getBpmStartDraft(definitionId);
    applyDraft(draft);
  } catch (error: any) {
    loadErrorMessage.value = isResubmitMode.value
      ? '当前实例已不可重新提交，请返回我的申请选择仍处于待重交状态的流程。'
      : '流程发起草稿加载失败，请重新加载后再试。';
    ElMessage.error(error?.message || loadErrorMessage.value);
  } finally {
    loading.value = false;
  }
}

function handleBack() {
  void router.push(
    isResubmitMode.value
      ? '/system/bpm/runtime/my-instance-list'
      : '/system/bpm/runtime/startable-list',
  );
}

async function validateBaseForm() {
  if (!formRef.value) {
    return true;
  }

  return formRef.value
    .validate()
    .then(() => true)
    .catch(() => false);
}

async function handleSubmit() {
  if (!loaded.value) {
    ElMessage.warning('请先等待流程草稿加载完成');
    return;
  }

  const submitInstanceId = formState.sourceInstanceId || routeInstanceId.value;
  if (!formState.definitionId || (isResubmitMode.value && !submitInstanceId)) {
    ElMessage.warning('流程草稿上下文无效，请重新加载后再试');
    return;
  }

  const baseFormValid = await validateBaseForm();
  if (!baseFormValid) {
    ElMessage.warning('请先完善流程基本信息');
    return;
  }

  submitting.value = true;
  try {
    const runtimeData =
      (await runtimeFormRef.value?.submit()) ?? cloneJson(runtimeFormData.value);
    const formDataJson = JSON.stringify(runtimeData ?? {});

    if (isResubmitMode.value) {
      const params: BpmInstanceResubmitForm = {
        formDataJson,
        instanceId: submitInstanceId,
        summary: formState.summary,
        title: formState.title,
      };
      await resubmitMyBpmInstance(params);
      ElMessage.success('流程已重新提交');
    } else {
      const params: BpmInstanceStartForm = {
        definitionId: formState.definitionId,
        formDataJson,
        summary: formState.summary,
        title: formState.title,
      };
      await startBpmInstance(params);
      ElMessage.success('流程已发起');
    }

    await router.push('/system/bpm/runtime/my-instance-list');
  } catch (error: any) {
    if (error?.message === 'FORM_VALIDATION_FAILED') {
      ElMessage.warning('请先完成运行表单中的必填项');
      return;
    }
    ElMessage.error(
      error?.message || (isResubmitMode.value ? '流程重新提交失败' : '流程发起失败'),
    );
  } finally {
    submitting.value = false;
  }
}

watch(
  [routeDefinitionId, routeInstanceId],
  () => {
    void loadDraft();
  },
  { immediate: true },
);
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtEditPage :title="pageTitle">
      <template #back>
        <ElButton link type="primary" @click="handleBack">{{ backLabel }}</ElButton>
      </template>

      <template #extra>
        <ElTag :type="loadErrorMessage ? 'danger' : loaded ? 'success' : 'info'" effect="light" round>
          {{ loadErrorMessage ? '加载失败' : loaded ? '已加载草稿' : '等待加载' }}
        </ElTag>
        <ElTag :type="isResubmitMode ? 'warning' : 'primary'" effect="light" round>
          {{ isResubmitMode ? '重新提交' : '首次发起' }}
        </ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <div v-if="loadErrorMessage" class="runtime-start-form__error">
        <ElEmpty :description="loadErrorMessage" />
      </div>
      <ElForm
        v-else
        ref="formRef"
        class="runtime-start-form"
        :disabled="loading || submitting"
        :model="formState"
        :rules="formRules"
        label-position="top"
      >
        <ArtEditSection title="流程信息" :index="1">
          <ElFormItem label="流程名称">
            <ElInput :model-value="formState.definitionName" disabled />
          </ElFormItem>
          <ElFormItem label="表单快照">
            <ElInput :model-value="formState.formNameSnapshot || '-'" disabled />
          </ElFormItem>
          <ElFormItem v-if="isResubmitMode" label="来源实例 ID">
            <ElInput
              :model-value="formState.sourceInstanceId ? String(formState.sourceInstanceId) : '-'"
              disabled
            />
          </ElFormItem>
          <ElFormItem label="流程标题" prop="title">
            <ElInput v-model="formState.title" clearable placeholder="请输入流程标题" />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="流程摘要" prop="summary">
            <ElInput
              v-model="formState.summary"
              maxlength="500"
              placeholder="请输入流程摘要"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="运行表单" :index="2">
          <ElFormItem class="art-edit-section__full" label="表单内容">
            <BpmRuntimeFormRenderer
              ref="runtimeFormRef"
              :key="runtimeFormKey"
              v-model="runtimeFormData"
              :disabled="loading || submitting"
              :schema-json="formState.formSchemaSnapshotJson"
            />
          </ElFormItem>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.runtime-start-form {
  display: grid;
  gap: 12px;
}

.runtime-start-form__error {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 320px;
}

.runtime-start-form :deep(.el-textarea__inner) {
  min-height: 72px !important;
}
</style>
