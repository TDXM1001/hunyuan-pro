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

function handleUpdateTitle() {
  drawerApi.setState({ title: '外部动态标题' }).open();
}
</script>

<template>
  <div>
    <Drawer />

    <HunyuanButton @click="open">Open</HunyuanButton>
    <HunyuanButton class="ml-2" type="primary" @click="handleUpdateTitle">
      从外部修改标题并打开
    </HunyuanButton>
  </div>
</template>
