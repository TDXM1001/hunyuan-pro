<script setup lang="ts">
import { provide } from 'vue';

import {
  platformConfigurationRequestClientKey,
} from '@hunyuan/feature-platform-configuration';
import PlatformConfigurationDictionaryPage from '@hunyuan/feature-platform-configuration/dictionary-page';

import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformConfigurationDictionaryEntry' });

// 字典页面与参数配置共享同一 feature，但每个懒加载入口独立完成应用装配。
provide(platformConfigurationRequestClientKey, requestClient);
</script>

<template>
  <PlatformConfigurationDictionaryPage />
</template>
