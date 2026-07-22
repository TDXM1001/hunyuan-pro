<script setup lang="ts">
import { provide } from 'vue';

import { accessClientKey, createAccessClient } from '@hunyuan/feature-access';
import AccessMenuPage from '@hunyuan/feature-access/menu-page';

import { requestClient } from '#/api/request';

defineOptions({ name: 'SystemMenuManagement' });

provide(accessClientKey, createAccessClient(requestClient));
</script>

<template>
  <AccessMenuPage />
</template>
