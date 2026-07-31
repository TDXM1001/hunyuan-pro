<script setup lang="ts">
import type { VbenFormSchema } from '@vben-core/form-ui';

import { computed, onMounted, ref } from 'vue';

import { ProfilePasswordSetting, z } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useIdentityAccountClient } from '../dependencies';

const passwordPolicyText = ref('正在读取密码策略...');
const accountClient = useIdentityAccountClient();

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'oldPassword',
      label: '旧密码',
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: '请输入旧密码',
      },
    },
    {
      fieldName: 'newPassword',
      label: '新密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请输入新密码',
      },
    },
    {
      fieldName: 'confirmPassword',
      label: '确认密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请再次输入新密码',
      },
      dependencies: {
        rules(values) {
          const { newPassword } = values;
          return z
            .string({ required_error: '请再次输入新密码' })
            .min(1, { message: '请再次输入新密码' })
            .refine((value) => value === newPassword, {
              message: '两次输入的密码不一致',
            });
        },
        triggerFields: ['newPassword'],
      },
    },
  ];
});

// 密码复杂度由服务端配置决定，页面初始化后再展示当前生效策略。
onMounted(async () => {
  const complexityEnabled = await accountClient.getPasswordPolicy();
  passwordPolicyText.value = complexityEnabled
    ? '密码复杂度校验已启用'
    : '密码复杂度校验未启用';
});

async function handleSubmit(values: Record<string, string>) {
  // 密码策略由后端返回并在表单侧提示，真正的复杂度校验仍以修改密码接口为准。
  await accountClient.changePassword({
    newPassword: values.newPassword ?? '',
    oldPassword: values.oldPassword ?? '',
  });
  ElMessage.success('密码修改成功');
}
</script>
<template>
  <div class="w-1/3">
    <p class="mb-4 text-sm text-muted-foreground">{{ passwordPolicyText }}</p>
    <ProfilePasswordSetting
      :form-schema="formSchema"
      @submit="handleSubmit"
    />
  </div>
</template>
