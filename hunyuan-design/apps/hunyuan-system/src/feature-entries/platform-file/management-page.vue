<script setup lang="ts">
import { provide } from 'vue';

import { platformFileRequestClientKey } from '@hunyuan/feature-platform-file';
import PlatformFileManagementPage from '@hunyuan/feature-platform-file/management-page';

import { requestClient } from '#/api/request';

defineOptions({ name: 'PlatformFileManagementEntry' });

// 文件管理的请求协议由应用提供，页面与查询模型归 platform-file feature。
provide(platformFileRequestClientKey, requestClient);
</script>

<template>
  <PlatformFileManagementPage />
</template>
