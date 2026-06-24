<script lang="ts" setup>
import { useHunyuanModal, HunyuanButton } from '@hunyuan/common-ui';

const [SlideModal, slideModalApi] = useHunyuanModal({
  animationType: 'slide',
});

const [ScaleModal, scaleModalApi] = useHunyuanModal({
  animationType: 'scale',
});

function openSlideModal() {
  slideModalApi.open();
}

function openScaleModal() {
  scaleModalApi.open();
}
</script>

<template>
  <div class="space-y-4">
    <div class="flex gap-4">
      <HunyuanButton @click="openSlideModal">滑动动画</HunyuanButton>
      <HunyuanButton @click="openScaleModal">缩放动画</HunyuanButton>
    </div>

    <SlideModal title="滑动动画示例" class="w-125">
      <p>这是使用滑动动画的弹窗，从顶部向下滑动进入。</p>
    </SlideModal>

    <ScaleModal title="缩放动画示例" class="w-125">
      <p>这是使用缩放动画的弹窗，以缩放淡入淡出的方式显示。</p>
    </ScaleModal>
  </div>
</template>
