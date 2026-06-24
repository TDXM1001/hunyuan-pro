<script lang="ts" setup>
import { HunyuanDescriptions } from '@hunyuan/common-ui';

const items = [
  { content: 'Hunyuan', label: '用户名' },
  { content: '13800138000', label: '手机号' },
  { content: '中国 · 杭州', label: '居住地' },
  { content: '这是一段较长的备注信息。', label: '备注', span: 3 },
];
</script>
<template>
  <HunyuanDescriptions bordered layout="vertical" :items="items" />
</template>
