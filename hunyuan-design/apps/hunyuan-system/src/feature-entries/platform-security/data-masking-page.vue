<script setup lang="ts">
import { provide } from 'vue';
import { platformSecurityRequestClientKey } from '@hunyuan/feature-platform-security';
import PlatformSecurityDataMaskingPage from '@hunyuan/feature-platform-security/data-masking-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformSecurityDataMaskingEntry' });
provide(platformSecurityRequestClientKey, requestClient);
</script>
<template><PlatformSecurityDataMaskingPage /></template>
