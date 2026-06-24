<script lang="ts" setup>
import { useHunyuanDrawer, HunyuanButton } from '@hunyuan/common-ui';

import ExtraDrawer from './drawer.vue';

const [Drawer, drawerApi] = useHunyuanDrawer({
  // 连接抽离的组件
  connectedComponent: ExtraDrawer,
});

function open() {
  drawerApi
    .setData({
      content: '外部传递的数据 content',
      payload: '外部传递的数据 payload',
    })
    .open();
}
</script>

<template>
  <div>
    <Drawer />

    <HunyuanButton @click="open">Open</HunyuanButton>
  </div>
</template>
