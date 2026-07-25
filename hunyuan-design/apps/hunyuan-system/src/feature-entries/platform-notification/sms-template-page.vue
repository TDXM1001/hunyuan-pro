<script setup lang="ts">
import { provide } from 'vue';
import { platformNotificationRequestClientKey } from '@hunyuan/feature-platform-notification';
import PlatformNotificationSmsTemplatePage from '@hunyuan/feature-platform-notification/sms-template-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformNotificationSmsTemplateEntry' });
provide(platformNotificationRequestClientKey, requestClient);
</script>
<template><PlatformNotificationSmsTemplatePage /></template>
