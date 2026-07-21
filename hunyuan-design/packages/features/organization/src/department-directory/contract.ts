export interface DepartmentRecord {
  createTime?: null | string;
  departmentId: number;
  departmentName: string;
  managerId?: null | number;
  managerName?: null | string;
  parentId: number;
  sort: number;
  updateTime?: null | string;
}

export interface DepartmentCommand {
  departmentName: string;
  managerId?: null | number;
  parentId: number;
  sort: number;
}

export interface OrganizationMember {
  actualName: string;
  departmentId: number;
  employeeId: number;
}

export interface OrganizationDepartmentClient {
  create(command: DepartmentCommand): Promise<number>;
  delete(departmentId: number): Promise<void>;
  list(): Promise<DepartmentRecord[]>;
  listManagers(): Promise<OrganizationMember[]>;
  update(departmentId: number, command: DepartmentCommand): Promise<void>;
}
