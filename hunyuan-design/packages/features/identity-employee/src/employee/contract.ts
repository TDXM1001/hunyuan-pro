/** 员工管理 feature 的数据模型和操作边界。员工主键、一次性密码和组织归属都在这里明确表达。 */
export interface PageResult<T> {
  /** 分页接口的统一返回结构。 */
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface EmployeeSummary {
  /** 后端员工列表返回的基础字段，disabled 是历史接口使用的状态名。 */
  actualName: string;
  avatar?: null | string;
  createTime?: null | string;
  departmentId?: null | number;
  departmentName?: null | string;
  disabled?: boolean;
  email?: null | string;
  employeeId: number;
  gender?: null | number;
  loginName: string;
  phone?: null | string;
  positionId?: null | number;
}

export type EmployeeRecord = Omit<EmployeeSummary, 'disabled'> & {
  /** 页面统一消费 disabledFlag，避免模板同时兼容两个状态字段。 */
  disabledFlag?: boolean;
  positionName?: null | string;
};

export interface EmployeeQueryParams {
  /** 员工列表筛选条件；pageNum/pageSize 始终由表格分页器提供。 */
  departmentId?: null | number;
  disabled?: boolean;
  keyword?: string;
  pageNum: number;
  pageSize: number;
}

export interface EmployeeCreateCommand {
  /** 新增员工时的完整表单数据，departmentId 是必选的组织归属。 */
  actualName: string;
  departmentId: number;
  disabled: boolean;
  email: string;
  gender?: null | number;
  loginName: string;
  phone: string;
  positionId?: null | number;
  remark?: null | string;
}

export interface EmployeeUpdateCommand extends EmployeeCreateCommand {
  /** 更新命令在新增字段基础上增加 employeeId，用来定位原员工。 */
  employeeId: number;
}

export interface EmployeeDepartmentAssignmentCommand {
  departmentId: number;
  employeeIds: number[];
}

export interface EmployeeDeleteCommand {
  employeeIds: number[];
}

export interface EmployeeOneTimeCredential {
  /** 新增或重置密码接口返回的一次性凭据，只应短暂展示给当前操作人。 */
  employeeId?: number;
  temporaryPassword: string;
}

export interface DepartmentOption {
  departmentId: number;
  departmentName: string;
  parentId: number;
  sort: number;
}

export interface PositionOption {
  positionId: number;
  positionName: string;
  sort?: null | number;
}

export interface ReadonlyDirectoryProvider<T> {
  /** 员工页面需要的部门/岗位只读目录，不允许通过此接口修改组织数据。 */
  list(): Promise<T[]>;
}

export interface EmployeeClient {
  /** 员工页面可执行的查询、保存、启停、分配和密码重置动作。 */
  assignDepartment(
    command: EmployeeDepartmentAssignmentCommand,
  ): Promise<void>;
  create(command: EmployeeCreateCommand): Promise<EmployeeOneTimeCredential>;
  delete(command: EmployeeDeleteCommand | number[]): Promise<void>;
  disable(employeeId: number): Promise<void>;
  enable(employeeId: number): Promise<void>;
  query(params: EmployeeQueryParams): Promise<PageResult<EmployeeRecord>>;
  resetPassword(employeeId: number): Promise<EmployeeOneTimeCredential>;
  update(command: EmployeeUpdateCommand): Promise<void>;
}
