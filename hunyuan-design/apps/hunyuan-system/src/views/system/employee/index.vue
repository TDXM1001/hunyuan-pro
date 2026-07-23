<script setup lang="ts">
import { provide } from 'vue';

import {
  createIdentityEmployeeClient,
  employeeClientKey,
  employeeDepartmentProviderKey,
  employeePositionProviderKey,
} from '@hunyuan/feature-identity-employee';
import IdentityEmployeePage from '@hunyuan/feature-identity-employee/employee-page';
import {
  createOrganizationDepartmentClient,
  createOrganizationPositionClient,
} from '@hunyuan/feature-organization';

import { requestClient } from '#/api/request';

defineOptions({ name: 'SystemEmployeeIndex' });

const organizationDepartmentClient =
  createOrganizationDepartmentClient(requestClient);
const organizationPositionClient =
  createOrganizationPositionClient(requestClient);

provide(employeeClientKey, createIdentityEmployeeClient(requestClient));
provide(employeeDepartmentProviderKey, {
  list: () => organizationDepartmentClient.list(),
});
provide(employeePositionProviderKey, {
  list: () => organizationPositionClient.list(),
});
</script>

<template>
  <IdentityEmployeePage />
</template>
