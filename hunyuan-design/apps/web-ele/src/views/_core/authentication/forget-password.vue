<script lang="ts" setup>
import type { HunyuanFormSchema } from '@hunyuan/common-ui';
import type { Recordable } from '@hunyuan/types';

import { computed, ref } from 'vue';

import { AuthenticationForgetPassword, z } from '@hunyuan/common-ui';
import { $t } from '@hunyuan/locales';

defineOptions({ name: 'ForgetPassword' });

const loading = ref(false);

const formSchema = computed((): HunyuanFormSchema[] => {
  return [
    {
      component: 'HunyuanInput',
      componentProps: {
        placeholder: 'example@example.com',
      },
      fieldName: 'email',
      label: $t('authentication.email'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.emailTip') })
        .email($t('authentication.emailValidErrorTip')),
    },
  ];
});

function handleSubmit(value: Recordable<any>) {
  void value;
}
</script>

<template>
  <AuthenticationForgetPassword
    :form-schema="formSchema"
    :loading="loading"
    @submit="handleSubmit"
  />
</template>
