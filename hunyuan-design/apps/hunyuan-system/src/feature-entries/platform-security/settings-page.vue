<script setup lang="ts">
import { provide } from 'vue';
import { platformSecurityRequestClientKey } from '@hunyuan/feature-platform-security';
import PlatformSecuritySettingsPage from '@hunyuan/feature-platform-security/settings-page';
import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformSecuritySettingsEntry' });
provide(platformSecurityRequestClientKey, requestClient);
</script>
<template><PlatformSecuritySettingsPage /></template>
