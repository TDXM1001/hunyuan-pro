<script setup lang="ts">
import { provide } from 'vue';

import {
  createOrganizationDepartmentClient,
  organizationDepartmentClientKey,
} from '@hunyuan/feature-organization';
import DepartmentDirectory from '@hunyuan/feature-organization/department-directory';

import { requestClient } from '#/api/request';

defineOptions({ name: 'OrganizationDepartmentDirectoryEntry' });

provide(
  organizationDepartmentClientKey,
  createOrganizationDepartmentClient(requestClient),
);
</script>

<template>
  <DepartmentDirectory />
</template>
