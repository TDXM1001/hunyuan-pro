<script lang="ts" setup>
import { ref } from 'vue';

import { useHunyuanModal, HunyuanButton } from '@hunyuan/common-ui';

const list = ref<number[]>([]);

const [Modal, modalApi] = useHunyuanModal({
  onCancel() {
    modalApi.close();
  },
  onConfirm() {
    console.log('onConfirm');
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      handleUpdate(10);
    }
  },
});

function handleUpdate(len: number) {
  modalApi.setState({ loading: true });
  setTimeout(() => {
    list.value = Array.from({ length: len }, (_v, k) => k + 1);
    modalApi.setState({ loading: false });
  }, 2000);
}
</script>
<template>
  <Modal title="自动计算高度">
    <div
      v-for="item in list"
      :key="item"
      class="flex-center h-55 w-full bg-muted even:bg-heavy"
    >
      {{ item }}
    </div>
    <template #prepend-footer>
      <HunyuanButton type="link" @click="handleUpdate(6)">
        点击更新数据
      </HunyuanButton>
    </template>
  </Modal>
</template>
