<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';

import { computed, onMounted, ref } from 'vue';

import { ProfilePasswordSetting, z } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import {
  changeCurrentAccountPasswordApi,
  getCurrentAccountPasswordPolicyApi,
} from '#/api';

const passwordPolicyText = ref('正在读取密码策略...');

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

onMounted(async () => {
  const complexityEnabled = await getCurrentAccountPasswordPolicyApi();
  passwordPolicyText.value = complexityEnabled
    ? '密码复杂度校验已启用'
    : '密码复杂度校验未启用';
});

async function handleSubmit(values: Record<string, string>) {
  await changeCurrentAccountPasswordApi({
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
