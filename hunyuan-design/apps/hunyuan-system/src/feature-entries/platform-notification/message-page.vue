<script setup lang="ts">
import { provide } from 'vue';
import { platformNotificationRequestClientKey } from '@hunyuan/feature-platform-notification';
import PlatformNotificationMessagePage from '@hunyuan/feature-platform-notification/message-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformNotificationMessageEntry' });
provide(platformNotificationRequestClientKey, requestClient);
</script>
<template><PlatformNotificationMessagePage /></template>
