import type { RequestClient } from '@vben/request';

import type {
  DepartmentCommand,
  DepartmentRecord,
  OrganizationMember,
  OrganizationDepartmentClient,
} from './contract';

const BASE_PATH = '/admin/v1/organization/departments';

// 页面只依赖注入的请求客户端，客户端工厂负责集中维护组织目录的稳定 API 路径。
/** 创建部门目录客户端，页面通过它完成部门和负责人选项的读写。 */
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

/** 将部门表单转换为后端命令：空负责人用 null 清除，顶级部门用 0 表示。 */
function normalize(command: DepartmentCommand): DepartmentCommand {
  // 顶级部门在后端以 0 表示根节点，负责人为空时显式发送 null 以清除旧值。
  return {
    ...command,
    departmentName: command.departmentName.trim(),
    managerId: command.managerId ?? null,
    parentId: command.parentId ?? 0,
  };
}
