<script setup lang="ts">
import { inject } from 'vue';

import { useScrollLock } from '@hunyuan-core/composables';
import { cn } from '@hunyuan-core/shared/utils';

const props = withDefaults(
  defineProps<{
    class?: any;
    overlayBlur?: number;
    position?: 'absolute' | 'fixed';
    zIndex?: number;
  }>(),
  {
    position: 'fixed',
  },
);

useScrollLock();
const dismissableModalId = inject('DISMISSABLE_MODAL_ID', undefined);
</script>

<template>
  <div
    :data-dismissable-modal="dismissableModalId"
    :style="{
      ...(zIndex ? { zIndex } : {}),
      position,
      backdropFilter:
        overlayBlur && overlayBlur > 0 ? `blur(${overlayBlur}px)` : 'none',
    }"
    :class="cn('z-popup bg-overlay inset-0 fixed', props.class)"
  ></div>
</template>
