<script setup lang="ts">
import { provide } from 'vue';
import { platformRuntimeRequestClientKey } from '@hunyuan/feature-platform-runtime';
import FeaturePage from '@hunyuan/feature-platform-runtime/serial-number-page';
import { requestClient } from '#/api/request';
defineOptions({ name: 'PlatformRuntimeSerialNumberEntry' });
provide(platformRuntimeRequestClientKey, requestClient);
</script>
<template><FeaturePage /></template>
