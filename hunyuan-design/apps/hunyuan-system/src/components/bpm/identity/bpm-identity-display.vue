<script setup lang="ts">import type { BpmIdentityReference } from '#/api/system/bpm/policy'; defineProps<{ value?: BpmIdentityReference }>();</script>
<template><span>{{ value?.displayName || '未选择' }}</span></template>
