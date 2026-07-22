export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface EmployeeSummary {
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
  disabledFlag?: boolean;
  positionName?: null | string;
};

export interface EmployeeQueryParams {
  departmentId?: null | number;
  disabled?: boolean;
  keyword?: string;
  pageNum: number;
  pageSize: number;
}

export interface EmployeeCreateCommand {
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
  list(): Promise<T[]>;
}

export interface EmployeeClient {
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
