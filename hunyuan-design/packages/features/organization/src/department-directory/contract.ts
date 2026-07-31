/** 部门目录的记录、表单命令和客户端接口。部门树由 parentId 组织起来。 */
export interface DepartmentRecord {
  /** 页面展示的部门记录；parentId 为 0 表示顶级部门。 */
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
  /** 新增/修改部门的可编辑字段，不包含后端生成的部门主键。 */
  departmentName: string;
  managerId?: null | number;
  parentId: number;
  sort: number;
}

export interface OrganizationMember {
  /** 可作为部门负责人的员工选项，只保留选择器需要的最小字段。 */
  actualName: string;
  departmentId: number;
  employeeId: number;
}

export interface OrganizationDepartmentClient {
  /** 部门目录页面使用的增删改查动作。 */
  create(command: DepartmentCommand): Promise<number>;
  delete(departmentId: number): Promise<void>;
  list(): Promise<DepartmentRecord[]>;
  listManagers(): Promise<OrganizationMember[]>;
  update(departmentId: number, command: DepartmentCommand): Promise<void>;
}
