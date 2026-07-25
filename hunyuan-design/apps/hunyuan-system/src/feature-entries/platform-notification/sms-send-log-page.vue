<script setup lang="ts">
import { provide } from 'vue';
import { platformNotificationRequestClientKey } from '@hunyuan/feature-platform-notification';
import PlatformNotificationSmsSendLogPage from '@hunyuan/feature-platform-notification/sms-send-log-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformNotificationSmsSendLogEntry' });
provide(platformNotificationRequestClientKey, requestClient);
</script>
<template><PlatformNotificationSmsSendLogPage /></template>
