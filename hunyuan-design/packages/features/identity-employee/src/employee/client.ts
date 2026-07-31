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

// 查询参数和写入参数分别归一化，避免搜索空格进入筛选条件，同时保留后端可识别的分页字段。
/** 把员工列表页面的筛选和分页状态转换成后端查询参数。 */
export function buildEmployeeQueryPayload(params: EmployeeQueryParams) {
  return {
    departmentId: params.departmentId ?? undefined,
    disabled: params.disabled,
    keyword: params.keyword?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

/**
 * 统一处理新增和更新员工的文本字段。
 * 页面允许用户输入空格，但后端保存的账号、姓名、电话和邮箱不应带不可见首尾字符。
 */
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

/** 把部门分配表单转换成批量命令，并删除重复员工 ID。 */
export function buildDepartmentAssignmentPayload(
  params: EmployeeDepartmentAssignmentCommand,
): EmployeeDepartmentAssignmentCommand {
  // 部门分配是集合写入，重复员工 ID 没有额外语义，提交前统一去重。
  return {
    departmentId: params.departmentId,
    employeeIds: [...new Set(params.employeeIds)],
  };
}

/** 兼容数组和命令对象两种调用方式，并统一输出 employeeIds 数组。 */
export function buildDeletePayload(
  params: EmployeeDeleteCommand | number[],
): EmployeeDeleteCommand {
  // 同时兼容旧调用方传入数组和新调用方传入命令对象，边界处统一成后端命令格式。
  const employeeIds = Array.isArray(params) ? params : params.employeeIds;
  return { employeeIds: [...new Set(employeeIds)] };
}

/** 创建员工客户端，集中维护员工模块的所有 API 路径和返回类型。 */
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
        // 旧接口可能只返回 disabled，新页面统一消费 disabledFlag，避免模板判断分叉。
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
