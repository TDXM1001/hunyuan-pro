<script setup lang="ts">
import { provide } from 'vue';
import { platformAuditRequestClientKey } from '@hunyuan/feature-platform-audit';
import PlatformAuditOperationLogPage from '@hunyuan/feature-platform-audit/operation-log-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformAuditOperationLogEntry' });
provide(platformAuditRequestClientKey, requestClient);
</script>
<template><PlatformAuditOperationLogPage /></template>
