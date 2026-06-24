<script lang="ts" setup>
import { useHunyuanDrawer, HunyuanButton } from '@hunyuan/common-ui';

import ExtraDrawer from './drawer.vue';

const [Drawer, drawerApi] = useHunyuanDrawer({
  // 连接抽离的组件
  connectedComponent: ExtraDrawer,
});

function open() {
  drawerApi.open();
}
</script>

<template>
  <div>
    <Drawer />
    <HunyuanButton @click="open">Open</HunyuanButton>
  </div>
</template>
