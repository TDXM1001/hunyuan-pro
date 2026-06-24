<script lang="ts" setup>
import { HunyuanSpinner } from '@hunyuan-core/shadcn-ui';

import { useContentSpinner } from './use-content-spinner';

defineOptions({ name: 'LayoutContentSpinner' });

const { spinning } = useContentSpinner();
</script>
<template>
  <HunyuanSpinner :spinning="spinning" />
</template>
