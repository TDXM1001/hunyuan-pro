<script setup lang="ts">
import { provide } from 'vue';

import { accessClientKey, createAccessClient } from '@hunyuan/feature-access';
import AccessRolePage from '@hunyuan/feature-access/role-page';

import { requestClient } from '#/api/request';

defineOptions({ name: 'SystemRoleIndex' });

provide(accessClientKey, createAccessClient(requestClient));
</script>

<template>
  <AccessRolePage />
</template>
