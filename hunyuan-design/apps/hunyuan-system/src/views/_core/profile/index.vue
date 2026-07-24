<script setup lang="ts">
import { provide } from 'vue';

import {
  createIdentityAccountClient,
  identityAccountClientKey,
} from '@hunyuan/feature-identity-account';
import IdentityAccountProfilePage from '@hunyuan/feature-identity-account/profile-page';

import { requestClient } from '#/api/request';

defineOptions({ name: 'IdentityAccountProfileEntry' });

// 个人中心仍由应用路由承载，业务 API 通过注入交给 identity-account feature。
provide(identityAccountClientKey, createIdentityAccountClient(requestClient));
</script>

<template>
  <IdentityAccountProfilePage />
</template>
