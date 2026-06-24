<script lang="ts" setup>
import { useHunyuanModal, HunyuanButton } from '@hunyuan/common-ui';

const [Modal, modalApi] = useHunyuanModal();
</script>
<template>
  <div>
    <HunyuanButton @click="() => modalApi.open()">Open</HunyuanButton>
    <Modal class="w-150" title="基础示例"> modal content </Modal>
  </div>
</template>
