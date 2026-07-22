<script setup lang="ts">
import { provide } from 'vue';

import {
  createIdentityEmployeeClient,
  employeeClientKey,
  employeeDepartmentProviderKey,
  employeePositionProviderKey,
} from '@hunyuan/feature-identity-employee';
import IdentityEmployeePage from '@hunyuan/feature-identity-employee/employee-page';
import { createOrganizationDepartmentClient } from '@hunyuan/feature-organization';

import { requestClient } from '#/api/request';
import { listPositions } from '#/api/system/organization';

defineOptions({ name: 'SystemEmployeeIndex' });

const organizationDepartmentClient =
  createOrganizationDepartmentClient(requestClient);

provide(employeeClientKey, createIdentityEmployeeClient(requestClient));
provide(employeeDepartmentProviderKey, {
  list: () => organizationDepartmentClient.list(),
});
provide(employeePositionProviderKey, {
  list: () => listPositions(),
});
</script>

<template>
  <IdentityEmployeePage />
</template>
