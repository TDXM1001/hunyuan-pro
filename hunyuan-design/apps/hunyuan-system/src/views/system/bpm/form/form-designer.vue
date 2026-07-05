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
