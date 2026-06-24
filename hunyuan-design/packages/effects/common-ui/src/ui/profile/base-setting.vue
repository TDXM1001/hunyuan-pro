<script setup lang="ts">
import type { Recordable } from '@hunyuan/types';

import type { HunyuanFormSchema } from '@hunyuan-core/form-ui';

import { computed, reactive } from 'vue';

import { $t } from '@hunyuan/locales';

import { useHunyuanForm } from '@hunyuan-core/form-ui';
import { HunyuanButton } from '@hunyuan-core/shadcn-ui';

interface Props {
  formSchema?: HunyuanFormSchema[];
}

const props = withDefaults(defineProps<Props>(), {
  formSchema: () => [],
});

const emit = defineEmits<{
  submit: [Recordable<any>];
}>();

const [Form, formApi] = useHunyuanForm(
  reactive({
    commonConfig: {
      // 所有表单项
      componentProps: {
        class: 'w-full',
      },
    },
    layout: 'horizontal',
    schema: computed(() => props.formSchema),
    showDefaultActions: false,
  }),
);

async function handleSubmit() {
  const { valid } = await formApi.validate();
  const values = await formApi.getValues();
  if (valid) {
    emit('submit', values);
  }
}

defineExpose({
  getFormApi: () => formApi,
});
</script>
<template>
  <div @keydown.enter.prevent="handleSubmit">
    <Form />
    <HunyuanButton type="submit" class="mt-4" @click="handleSubmit">
      {{ $t('profile.updateBasicProfile') }}
    </HunyuanButton>
  </div>
</template>
