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

export interface PositionPageQueryParams {
  keywords?: string;
  pageNum: number;
  pageSize: number;
}

export interface PositionAddForm {
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort: number;
}

export interface PositionUpdateForm extends PositionAddForm {
  positionId: number;
}

export interface RoleAddForm {
  remark?: null | string;
  roleCode: string;
  roleName: string;
}

export interface RoleUpdateForm extends RoleAddForm {
  roleId: number;
}

export interface MenuSimpleTreeRecord {
  children?: MenuSimpleTreeRecord[];
  contextMenuId?: null | number;
  menuId: number;
  menuName: string;
  menuType: number;
  parentId: number;
}

export interface RoleMenuTreeRecord {
  menuTreeList: MenuSimpleTreeRecord[];
  roleId: number;
  selectedMenuId: number[];
}

export interface RoleMenuUpdateForm {
  menuIdList: number[];
  roleId: number;
}

export interface DataScopeViewTypeRecord {
  viewType: number;
  viewTypeLevel: number;
  viewTypeName: string;
}

export interface DataScopeRecord {
  dataScopeType: number;
  dataScopeTypeDesc: string;
  dataScopeTypeName: string;
  dataScopeTypeSort: number;
  viewTypeList: DataScopeViewTypeRecord[];
}

export interface RoleDataScopeRecord {
  dataScopeType: number;
  viewType: number;
}

export interface RoleDataScopeUpdateForm {
  dataScopeItemList: RoleDataScopeRecord[];
  roleId: number;
}

export interface RoleEmployeeQueryParams {
  keywords?: string;
  pageNum: number;
  pageSize: number;
  roleId: number;
}

export interface RoleEmployeeUpdateForm {
  employeeIdList: number[];
  roleId: number;
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

export async function listPositions() {
  return requestClient.get<PositionRecord[]>('/position/queryList');
}

export async function listRoles() {
  return requestClient.get<RoleRecord[]>('/role/getAll');
}

export function buildPositionMutationPayload<
  T extends PositionAddForm | PositionUpdateForm,
>(params: T): T {
  return {
    ...params,
    positionLevel: params.positionLevel?.trim() || '',
    positionName: params.positionName.trim(),
    remark: params.remark?.trim() || '',
  };
}

export function buildRoleMutationPayload<
  T extends RoleAddForm | RoleUpdateForm,
>(params: T): T {
  return {
    ...params,
    remark: params.remark?.trim() || '',
    roleCode: params.roleCode.trim(),
    roleName: params.roleName.trim(),
  };
}

export function buildRoleMenuPayload(
  params: RoleMenuUpdateForm,
): RoleMenuUpdateForm {
  return {
    menuIdList: [...params.menuIdList],
    roleId: params.roleId,
  };
}

export function buildRoleDataScopePayload(
  params: RoleDataScopeUpdateForm,
): RoleDataScopeUpdateForm {
  return {
    dataScopeItemList: params.dataScopeItemList.map((item) => ({
      dataScopeType: item.dataScopeType,
      viewType: item.viewType,
    })),
    roleId: params.roleId,
  };
}

export function buildRoleEmployeePayload(
  params: RoleEmployeeUpdateForm,
): RoleEmployeeUpdateForm {
  return {
    employeeIdList: [...new Set(params.employeeIdList)],
    roleId: params.roleId,
  };
}

export function buildRoleEmployeeQueryPayload(params: RoleEmployeeQueryParams) {
  return {
    keywords: params.keywords?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    roleId: String(params.roleId),
  };
}

export async function queryPositionPage(params: PositionPageQueryParams) {
  return requestClient.post<PageResult<PositionRecord>>('/position/queryPage', {
    keywords: params.keywords?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  });
}

export async function addPosition(params: PositionAddForm) {
  return requestClient.post<string>(
    '/position/add',
    buildPositionMutationPayload(params),
  );
}

export async function updatePosition(params: PositionUpdateForm) {
  return requestClient.post<string>(
    '/position/update',
    buildPositionMutationPayload(params),
  );
}

export async function deletePosition(positionId: number) {
  return requestClient.get<string>(`/position/delete/${positionId}`);
}

export async function batchDeletePositions(positionIds: number[]) {
  return requestClient.post<string>('/position/batchDelete', positionIds);
}

export async function addRole(params: RoleAddForm) {
  return requestClient.post<string>('/role/add', buildRoleMutationPayload(params));
}

export async function updateRole(params: RoleUpdateForm) {
  return requestClient.post<string>(
    '/role/update',
    buildRoleMutationPayload(params),
  );
}

export async function deleteRole(roleId: number) {
  return requestClient.get<string>(`/role/delete/${roleId}`);
}

export async function getRole(roleId: number) {
  return requestClient.get<RoleRecord>(`/role/get/${roleId}`);
}

export async function getRoleSelectedMenu(roleId: number) {
  return requestClient.get<RoleMenuTreeRecord>(
    `/role/menu/getRoleSelectedMenu/${roleId}`,
  );
}

export async function updateRoleMenu(params: RoleMenuUpdateForm) {
  return requestClient.post<string>(
    '/role/menu/updateRoleMenu',
    buildRoleMenuPayload(params),
  );
}

export async function listDataScopes() {
  return requestClient.get<DataScopeRecord[]>('/dataScope/list');
}

export async function getRoleDataScopeList(roleId: number) {
  return requestClient.get<RoleDataScopeRecord[]>(
    `/role/dataScope/getRoleDataScopeList/${roleId}`,
  );
}

export async function updateRoleDataScopeList(
  params: RoleDataScopeUpdateForm,
) {
  return requestClient.post<string>(
    '/role/dataScope/updateRoleDataScopeList',
    buildRoleDataScopePayload(params),
  );
}

export async function queryRoleEmployees(params: RoleEmployeeQueryParams) {
  return requestClient.post<PageResult<EmployeeRecord>>(
    '/role/employee/queryEmployee',
    buildRoleEmployeeQueryPayload(params),
  );
}

export async function queryCandidateRoleEmployees(
  params: RoleEmployeeQueryParams,
) {
  return requestClient.post<PageResult<EmployeeRecord>>(
    '/role/employee/queryCandidateEmployee',
    buildRoleEmployeeQueryPayload(params),
  );
}

export async function listAllEmployeesByRoleId(roleId: number) {
  return requestClient.get<EmployeeRecord[]>(
    `/role/employee/getAllEmployeeByRoleId/${roleId}`,
  );
}

export async function removeRoleEmployee(params: {
  employeeId: number;
  roleId: number;
}) {
  return requestClient.get<string>(
    `/role/employee/removeEmployee?employeeId=${params.employeeId}&roleId=${params.roleId}`,
  );
}

export async function batchRemoveRoleEmployees(
  params: RoleEmployeeUpdateForm,
) {
  return requestClient.post<string>(
    '/role/employee/batchRemoveRoleEmployee',
    buildRoleEmployeePayload(params),
  );
}

export async function batchAddRoleEmployees(params: RoleEmployeeUpdateForm) {
  return requestClient.post<string>(
    '/role/employee/batchAddRoleEmployee',
    buildRoleEmployeePayload(params),
  );
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
