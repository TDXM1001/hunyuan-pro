import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface EmployeeRecord {
  actualName: string;
  administratorFlag?: boolean;
  avatar?: null | string;
  createTime?: null | string;
  departmentId?: null | number;
  departmentName?: null | string;
  disabledFlag?: boolean;
  email?: null | string;
  employeeId: number;
  gender?: null | number;
  loginName: string;
  phone?: null | string;
  positionId?: null | number;
  positionName?: null | string;
  roleIdList?: number[];
  roleNameList?: string[];
}

export interface DepartmentRecord {
  departmentId: number;
  departmentName: string;
  managerId?: null | number;
  managerName?: null | string;
  parentId?: null | number;
  sort?: null | number;
}

export interface PositionRecord {
  positionId: number;
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort?: null | number;
}

export interface RoleRecord {
  remark?: null | string;
  roleCode?: null | string;
  roleId: number;
  roleName: string;
}

export interface EmployeeQueryParams {
  departmentId?: null | number;
  disabledFlag?: boolean;
  keyword?: string;
  pageNum: number;
  pageSize: number;
}

export async function queryEmployeePage(params: EmployeeQueryParams) {
  return requestClient.post<PageResult<EmployeeRecord>>('/employee/query', {
    departmentId: params.departmentId ?? undefined,
    disabledFlag: params.disabledFlag,
    keyword: params.keyword?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  });
}

export async function listDepartments() {
  return requestClient.get<DepartmentRecord[]>('/department/listAll');
}

export async function listPositions() {
  return requestClient.get<PositionRecord[]>('/position/queryList');
}

export async function listRoles() {
  return requestClient.get<RoleRecord[]>('/role/getAll');
}

export interface EmployeeAddForm {
  actualName: string;
  loginName: string;
  departmentId: null | number;
  disabledFlag?: boolean;
  positionId?: null | number;
  roleIdList?: number[];
  phone: string;
  email: string;
  gender?: number;
}

export interface EmployeeUpdateForm {
  employeeId: number;
  actualName: string;
  loginName: string;
  departmentId: null | number;
  disabledFlag?: boolean;
  positionId?: null | number;
  roleIdList?: number[];
  phone: string;
  email: string;
  gender?: number;
}

export function buildEmployeeMutationPayload<
  T extends EmployeeAddForm | EmployeeUpdateForm,
>(params: T): T {
  return {
    ...params,
    actualName: params.actualName.trim(),
    disabledFlag: params.disabledFlag ?? false,
    email: params.email.trim(),
    loginName: params.loginName.trim(),
    phone: params.phone.trim(),
    roleIdList: params.roleIdList ?? [],
  };
}

export async function addEmployee(params: EmployeeAddForm) {
  return requestClient.post<string>(
    '/employee/add',
    buildEmployeeMutationPayload(params),
  );
}

export async function updateEmployee(params: EmployeeUpdateForm) {
  return requestClient.post<string>(
    '/employee/update',
    buildEmployeeMutationPayload(params),
  );
}

export async function toggleEmployeeStatus(employeeId: number) {
  return requestClient.get<string>(`/employee/update/disabled/${employeeId}`);
}

export async function batchDeleteEmployees(employeeIds: number[]) {
  return requestClient.post<string>('/employee/update/batch/delete', employeeIds);
}

export async function batchUpdateDepartment(params: {
  departmentId: number;
  employeeIdList: number[];
}) {
  return requestClient.post<string>('/employee/update/batch/department', params);
}
