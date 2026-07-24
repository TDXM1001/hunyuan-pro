<script setup lang="ts">
import { provide } from 'vue';

import {
  platformConfigurationRequestClientKey,
} from '@hunyuan/feature-platform-configuration';
import PlatformConfigurationPage from '@hunyuan/feature-platform-configuration/configuration-page';

import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformConfigurationEntry' });

// 应用只负责提供请求能力，参数配置的接口与页面均归 feature 所有。
provide(platformConfigurationRequestClientKey, requestClient);
</script>

<template>
  <PlatformConfigurationPage />
</template>
