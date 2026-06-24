<script lang="ts" setup>
import { HunyuanCountToAnimator } from '@hunyuan/common-ui';
</script>
<template>
  <HunyuanCountToAnimator
    :duration="3000"
    :end-val="2000000"
    :start-val="1"
    prefix="$"
    separator="/"
  />
</template>
