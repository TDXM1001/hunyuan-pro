<script lang="ts" setup>
import { useHunyuanModal } from '@hunyuan/common-ui';

const [Modal] = useHunyuanModal({
  draggable: true,
});
</script>
<template>
  <Modal title="拖拽示例"> modal content </Modal>
</template>
