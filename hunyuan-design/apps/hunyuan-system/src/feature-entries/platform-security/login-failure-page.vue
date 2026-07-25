<script setup lang="ts">
import { provide } from 'vue';
import { platformSecurityRequestClientKey } from '@hunyuan/feature-platform-security';
import PlatformSecurityLoginFailurePage from '@hunyuan/feature-platform-security/login-failure-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformSecurityLoginFailureEntry' });
provide(platformSecurityRequestClientKey, requestClient);
</script>
<template><PlatformSecurityLoginFailurePage /></template>
