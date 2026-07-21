import type { RequestClient } from '@vben/request';

import type {
  DepartmentCommand,
  DepartmentRecord,
  OrganizationMember,
  OrganizationDepartmentClient,
} from './contract';

const BASE_PATH = '/admin/v1/organization/departments';

export function createOrganizationDepartmentClient(
  requestClient: RequestClient,
): OrganizationDepartmentClient {
  return {
    create(command) {
      return requestClient.post<number>(BASE_PATH, normalize(command));
    },
    async delete(departmentId) {
      await requestClient.delete(`${BASE_PATH}/${departmentId}`);
    },
    list() {
      return requestClient.get<DepartmentRecord[]>(BASE_PATH);
    },
    listManagers() {
      return requestClient.get<OrganizationMember[]>(`${BASE_PATH}/manager-options`);
    },
    async update(departmentId, command) {
      await requestClient.put(`${BASE_PATH}/${departmentId}`, normalize(command));
    },
  };
}

function normalize(command: DepartmentCommand): DepartmentCommand {
  return {
    ...command,
    departmentName: command.departmentName.trim(),
    managerId: command.managerId ?? null,
    parentId: command.parentId ?? 0,
  };
}
