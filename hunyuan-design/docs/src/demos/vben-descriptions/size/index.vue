<script lang="ts" setup>
import { HunyuanDescriptions } from '@hunyuan/common-ui';

const items = [
  { content: 'Hunyuan', label: '用户名' },
  { content: '13800138000', label: '手机号' },
  { content: '中国 · 杭州', label: '居住地' },
  { content: '前端工程师', label: '职位' },
];
</script>
<template>
  <div style="display: flex; flex-direction: column; gap: 16px">
    <HunyuanDescriptions
      size="small"
      bordered
      title="Small"
      :column="2"
      :items="items"
    />
    <HunyuanDescriptions
      size="middle"
      bordered
      title="Middle"
      :column="2"
      :items="items"
    />
    <HunyuanDescriptions
      size="large"
      bordered
      title="Large"
      :column="2"
      :items="items"
    />
  </div>
</template>
