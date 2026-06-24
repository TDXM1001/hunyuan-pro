<script lang="ts" setup>
import { HunyuanDescriptions, HunyuanDescriptionsItem } from '@hunyuan/common-ui';
</script>
<template>
  <!-- 通过子组件 HunyuanDescriptionsItem 声明列表项 -->
  <HunyuanDescriptions bordered :column="2">
    <HunyuanDescriptionsItem label="用户名">Hunyuan</HunyuanDescriptionsItem>
    <HunyuanDescriptionsItem label="状态">
      <span style="color: #52c41a">● 正常</span>
    </HunyuanDescriptionsItem>
    <HunyuanDescriptionsItem label="备注" :span="2">
      <template #content>
        <span style="color: #888">通过 #content 插槽自定义内容</span>
      </template>
    </HunyuanDescriptionsItem>
  </HunyuanDescriptions>
</template>
