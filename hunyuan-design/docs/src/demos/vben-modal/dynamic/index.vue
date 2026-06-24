<script lang="ts" setup>
import { useHunyuanModal, HunyuanButton } from '@hunyuan/common-ui';

import ExtraModal from './modal.vue';

const [Modal, modalApi] = useHunyuanModal({
  // 连接抽离的组件
  connectedComponent: ExtraModal,
});

function openModal() {
  modalApi.open();
}

function handleUpdateTitle() {
  modalApi.setState({ title: '外部动态标题' }).open();
}
</script>

<template>
  <div>
    <Modal />

    <HunyuanButton @click="openModal">Open</HunyuanButton>
    <HunyuanButton class="ml-2" type="primary" @click="handleUpdateTitle">
      从外部修改标题并打开
    </HunyuanButton>
  </div>
</template>
