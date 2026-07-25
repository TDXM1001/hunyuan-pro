<script setup lang="ts">
import { provide } from 'vue';
import { platformDevtoolsRequestClientKey } from '@hunyuan/feature-platform-devtools';
import FeaturePage from '@hunyuan/feature-platform-devtools/api-encrypt-page';
import { requestClient } from '#/api/request';
defineOptions({ name: 'PlatformDevtoolsApiEncryptEntry' });
provide(platformDevtoolsRequestClientKey, requestClient);
</script>
<template><FeaturePage /></template>
