<script setup lang="ts">
import { provide } from 'vue';
import { platformRuntimeRequestClientKey } from '@hunyuan/feature-platform-runtime';
import FeaturePage from '@hunyuan/feature-platform-runtime/reload-page';
import { requestClient } from '#/api/request';
defineOptions({ name: 'PlatformRuntimeReloadEntry' });
provide(platformRuntimeRequestClientKey, requestClient);
</script>
<template><FeaturePage /></template>
