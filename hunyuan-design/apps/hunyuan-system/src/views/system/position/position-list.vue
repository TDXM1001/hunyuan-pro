<script setup lang="ts">
import { provide } from 'vue';

import {
  createOrganizationPositionClient,
  organizationPositionClientKey,
} from '@hunyuan/feature-organization';
import OrganizationPositionDirectory from '@hunyuan/feature-organization/position-directory';

import { requestClient } from '#/api/request';

defineOptions({ name: 'SystemPositionList' });

provide(
  organizationPositionClientKey,
  createOrganizationPositionClient(requestClient),
);
</script>

<template>
  <OrganizationPositionDirectory />
</template>
