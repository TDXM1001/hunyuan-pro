<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { BasicOption } from '@vben/types';

import { computed } from 'vue';

import { AuthenticationLogin, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();

const ACCOUNT_OPTIONS: BasicOption[] = [
  {
    label: 'Admin',
    value: 'admin',
  },
];

const LOGIN_DEVICE_PC = 1;

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenSelect',
      componentProps: {
        options: ACCOUNT_OPTIONS,
        placeholder: $t('authentication.selectAccount'),
      },
      fieldName: 'selectAccount',
      label: $t('authentication.selectAccount'),
      rules: z.string().optional().default('admin'),
    },
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      defaultValue: 'admin',
      dependencies: {
        trigger(values, form) {
          if (values.selectAccount === 'admin') {
            form.setValues({
              loginName: 'admin',
              password: '123456',
            });
          }
        },
        triggerFields: ['selectAccount'],
      },
      fieldName: 'loginName',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      defaultValue: '123456',
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
    {
      component: 'VbenInput',
      componentProps: {
        type: 'hidden',
      },
      defaultValue: LOGIN_DEVICE_PC,
      fieldName: 'loginDevice',
      formItemClass: 'hidden',
      label: 'loginDevice',
      rules: z.number().default(LOGIN_DEVICE_PC),
    },
  ];
});
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :show-code-login="false"
    :show-forget-password="false"
    :show-qrcode-login="false"
    :show-register="false"
    :show-third-party-login="false"
    @submit="authStore.authLogin"
  />
</template>
