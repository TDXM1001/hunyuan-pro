<script setup lang="ts">
import { provide } from 'vue';
import { platformRuntimeRequestClientKey } from '@hunyuan/feature-platform-runtime';
import FeaturePage from '@hunyuan/feature-platform-runtime/job-page';
import { requestClient } from '#/api/request';
defineOptions({ name: 'PlatformRuntimeJobEntry' });
provide(platformRuntimeRequestClientKey, requestClient);
</script>
<template><FeaturePage /></template>
