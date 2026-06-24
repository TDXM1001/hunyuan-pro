<script lang="ts" setup>
import { useHunyuanDrawer, HunyuanButton } from '@hunyuan/common-ui';

const [Drawer, drawerApi] = useHunyuanDrawer();
</script>
<template>
  <div>
    <HunyuanButton @click="() => drawerApi.open()">Open</HunyuanButton>
    <Drawer class="w-150" title="基础示例"> drawer content </Drawer>
  </div>
</template>
