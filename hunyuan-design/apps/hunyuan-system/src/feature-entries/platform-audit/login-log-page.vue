<script setup lang="ts">
import { provide } from 'vue';
import { platformAuditRequestClientKey } from '@hunyuan/feature-platform-audit';
import PlatformAuditLoginLogPage from '@hunyuan/feature-platform-audit/login-log-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformAuditLoginLogEntry' });
provide(platformAuditRequestClientKey, requestClient);
</script>
<template><PlatformAuditLoginLogPage /></template>
