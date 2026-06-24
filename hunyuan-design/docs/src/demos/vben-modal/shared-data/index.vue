<script lang="ts" setup>
import { useHunyuanModal, HunyuanButton } from '@hunyuan/common-ui';

import ExtraModal from './modal.vue';

const [Modal, modalApi] = useHunyuanModal({
  // 连接抽离的组件
  connectedComponent: ExtraModal,
});

function openModal() {
  modalApi
    .setData({
      content: '外部传递的数据 content',
      payload: '外部传递的数据 payload',
    })
    .open();
}
</script>

<template>
  <div>
    <Modal />

    <HunyuanButton @click="openModal">Open</HunyuanButton>
  </div>
</template>
