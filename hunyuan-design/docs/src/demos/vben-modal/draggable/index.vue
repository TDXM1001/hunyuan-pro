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
</script>

<template>
  <div>
    <Modal />
    <HunyuanButton @click="openModal">Open</HunyuanButton>
  </div>
</template>
