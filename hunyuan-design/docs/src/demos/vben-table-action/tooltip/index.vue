<script lang="ts" setup>
import type { ActionItem } from '@hunyuan/common-ui';

import { HunyuanTableAction } from '@hunyuan/common-ui';

const actions: ActionItem[] = [
  { key: 'edit', text: '编辑', tooltip: '编辑这一行' },
  {
    key: 'detail',
    text: '详情',
    tooltip: { content: '查看详情', side: 'top' },
  },
];
</script>
<template>
  <HunyuanTableAction :actions="actions" align="start" />
</template>
