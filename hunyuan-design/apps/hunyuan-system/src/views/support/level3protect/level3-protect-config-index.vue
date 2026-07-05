<script setup lang="ts">
import type { Level3ProtectConfigFormModel } from '#/api/system/network-protect';
import type { FormInstance, FormRules } from 'element-plus';
import type { ArtActionItem } from '@vben/art-hooks/common';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtPageActions } from '@vben/art-hooks/common';
import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';
import { Page } from '@vben/common-ui';

import {
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

function createDefaultFormData(): Level3ProtectConfigFormModel {
  return {
    fileDetectFlag: false,
    loginActiveTimeoutMinutes: 30,
    loginFailLockMinutes: 30,
    loginFailMaxTimes: 3,
    maxUploadFileSizeMb: 50,
    passwordComplexityEnabled: true,
    regularChangePasswordMonths: 3,
    regularChangePasswordNotAllowRepeatTimes: 3,
    twoFactorLoginEnabled: false,
  };
}

const formRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const loaded = ref(false);

const formData = reactive<Level3ProtectConfigFormModel>(createDefaultFormData());

const rules: FormRules<Level3ProtectConfigFormModel> = {
  loginFailMaxTimes: [{ required: true, message: '请输入连续失败次数', trigger: 'blur' }],
  loginFailLockMinutes: [{ required: true, message: '请输入锁定时长', trigger: 'blur' }],
  loginActiveTimeoutMinutes: [{ required: true, message: '请输入最大在线时长', trigger: 'blur' }],
  regularChangePasswordMonths: [{ required: true, message: '请输入定期改密周期', trigger: 'blur' }],
  regularChangePasswordNotAllowRepeatTimes: [
    { required: true, message: '请输入历史密码重复限制', trigger: 'blur' },
  ],
  maxUploadFileSizeMb: [{ required: true, message: '请输入上传大小限制', trigger: 'blur' }],
};

const pageActions = computed<ArtActionItem[]>(() => [
  {
    key: 'reset',
    label: '重置',
    onClick: handleReset,
  },
  {
    key: 'save',
    label: '保存配置',
    loading: saving.value,
    onClick: handleSave,
    type: 'primary',
  },
]);

async function loadConfig() {
  loading.value = true;
  try {
    const result = await queryLevel3ProtectConfig();
    Object.assign(formData, result);
    loaded.value = true;
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  void loadConfig();
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  saving.value = true;
  try {
    await updateLevel3ProtectConfig(formData);
    ElMessage.success('三级等保配置保存成功');
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  // 菜单直接进入配置页，首屏即加载后端当前配置。
  void loadConfig().catch((error) => {
    ElMessage.error(error?.message || '三级等保配置加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtEditPage title="三级等保设置">
      <template #extra>
        <ElTag :type="loaded ? 'success' : 'info'" effect="light" round>
          {{ loaded ? '已加载后端配置' : '等待加载' }}
        </ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <ElForm
        ref="formRef"
        class="level3-protect-form"
        :disabled="loading"
        :model="formData"
        :rules="rules"
        label-position="top"
      >
        <ArtEditSection title="登录安全" :index="1">
          <ElFormItem label="连续失败锁定次数" prop="loginFailMaxTimes">
            <ElInputNumber
              v-model="formData.loginFailMaxTimes"
              :min="1"
              placeholder="请输入连续失败锁定次数"
            />
          </ElFormItem>
          <ElFormItem label="锁定时长（分钟）" prop="loginFailLockMinutes">
            <ElInputNumber
              v-model="formData.loginFailLockMinutes"
              :min="1"
              placeholder="请输入锁定时长"
            />
          </ElFormItem>
          <ElFormItem label="最大在线时长（分钟）" prop="loginActiveTimeoutMinutes">
            <ElInputNumber
              v-model="formData.loginActiveTimeoutMinutes"
              :min="1"
              placeholder="请输入最大在线时长"
            />
          </ElFormItem>
          <ElFormItem label="双因子登录" prop="twoFactorLoginEnabled">
            <ElSwitch v-model="formData.twoFactorLoginEnabled" />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="密码与文件策略" :index="2">
          <ElFormItem label="启用密码复杂度" prop="passwordComplexityEnabled">
            <ElSwitch v-model="formData.passwordComplexityEnabled" />
          </ElFormItem>
          <ElFormItem label="定期改密周期（月）" prop="regularChangePasswordMonths">
            <ElInputNumber
              v-model="formData.regularChangePasswordMonths"
              :min="1"
              placeholder="请输入定期改密周期"
            />
          </ElFormItem>
          <ElFormItem
            label="历史密码重复限制（次）"
            prop="regularChangePasswordNotAllowRepeatTimes"
          >
            <ElInputNumber
              v-model="formData.regularChangePasswordNotAllowRepeatTimes"
              :min="1"
              placeholder="请输入历史密码重复限制"
            />
          </ElFormItem>
          <ElFormItem label="启用文件检测" prop="fileDetectFlag">
            <ElSwitch v-model="formData.fileDetectFlag" />
          </ElFormItem>
          <ElFormItem label="上传大小限制（MB）" prop="maxUploadFileSizeMb">
            <ElInputNumber
              v-model="formData.maxUploadFileSizeMb"
              :min="1"
              placeholder="请输入上传大小限制"
            />
          </ElFormItem>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.level3-protect-form {
  display: grid;
  gap: 12px;
}

.level3-protect-form :deep(.el-input-number) {
  width: 100%;
}
</style>
