import type { RequestClient } from '@vben/request';

import type {
  EmployeeCreateCommand,
  EmployeeDeleteCommand,
  EmployeeDepartmentAssignmentCommand,
  EmployeeOneTimeCredential,
  EmployeeQueryParams,
  EmployeeRecord,
  EmployeeSummary,
  EmployeeUpdateCommand,
  PageResult,
} from './contract';

import type { EmployeeClient } from './contract';

const BASE_PATH = '/admin/v1/identity/employees';

export function buildEmployeeQueryPayload(params: EmployeeQueryParams) {
  return {
    departmentId: params.departmentId ?? undefined,
    disabled: params.disabled,
    keyword: params.keyword?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export function buildEmployeeMutationPayload<
  T extends EmployeeCreateCommand | EmployeeUpdateCommand,
>(params: T): T {
  return {
    ...params,
    actualName: params.actualName.trim(),
    email: params.email.trim(),
    loginName: params.loginName.trim(),
    phone: params.phone.trim(),
    remark: params.remark?.trim() || '',
  };
}

export function buildDepartmentAssignmentPayload(
  params: EmployeeDepartmentAssignmentCommand,
): EmployeeDepartmentAssignmentCommand {
  return {
    departmentId: params.departmentId,
    employeeIds: [...new Set(params.employeeIds)],
  };
}

export function buildDeletePayload(
  params: EmployeeDeleteCommand | number[],
): EmployeeDeleteCommand {
  const employeeIds = Array.isArray(params) ? params : params.employeeIds;
  return { employeeIds: [...new Set(employeeIds)] };
}

export function createIdentityEmployeeClient(
  requestClient: RequestClient,
): EmployeeClient {
  return {
    assignDepartment(command) {
      return requestClient.post<void>(
        `${BASE_PATH}/department-assignment`,
        buildDepartmentAssignmentPayload(command),
      );
    },
    create(command) {
      return requestClient.post<EmployeeOneTimeCredential>(
        BASE_PATH,
        buildEmployeeMutationPayload(command),
      );
    },
    delete(command) {
      return requestClient.post<void>(
        `${BASE_PATH}/delete`,
        buildDeletePayload(command),
      );
    },
    disable(employeeId) {
      return requestClient.post<void>(`${BASE_PATH}/${employeeId}/disable`);
    },
    enable(employeeId) {
      return requestClient.post<void>(`${BASE_PATH}/${employeeId}/enable`);
    },
    async query(params) {
      const result = await requestClient.post<PageResult<EmployeeSummary>>(
        `${BASE_PATH}/query`,
        buildEmployeeQueryPayload(params),
      );
      return {
        ...result,
        list: result.list.map((item) => ({
          ...item,
          disabledFlag: item.disabled ?? false,
        })),
      } satisfies PageResult<EmployeeRecord>;
    },
    resetPassword(employeeId) {
      return requestClient.post<EmployeeOneTimeCredential>(
        `${BASE_PATH}/${employeeId}/password/reset`,
      );
    },
    update(command) {
      return requestClient.put<void>(
        `${BASE_PATH}/${command.employeeId}`,
        buildEmployeeMutationPayload(command),
      );
    },
  };
}
