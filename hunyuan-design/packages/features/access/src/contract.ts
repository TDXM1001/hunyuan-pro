export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pages: number;
  pageSize: number;
  total: number;
}

export interface AccessRoleRecord {
  remark?: null | string;
  roleCode: string;
  roleId: number;
  roleName: string;
}

export interface AccessRoleCommand {
  remark?: null | string;
  roleCode: string;
  roleName: string;
}

export interface AccessCapabilityNode {
  capabilityId: number;
  capabilityName: string;
  capabilityType: number;
  children?: AccessCapabilityNode[];
  contextCapabilityId?: null | number;
  parentId?: null | number;
}

export interface AccessRoleCapabilityGrant {
  capabilityTree: AccessCapabilityNode[];
  roleId: number;
  selectedCapabilityIds: number[];
}

export interface AccessDataScopeViewOption {
  level: number;
  name: string;
  viewType: number;
}

export interface AccessDataScopeDefinition {
  dataScopeType: number;
  dataScopeTypeDescription: string;
  dataScopeTypeName: string;
  sort: number;
  viewOptions: AccessDataScopeViewOption[];
}

export interface AccessDataScopeSetting {
  dataScopeType: number;
  viewType: number;
}

export interface AccessRoleDataScopes {
  dataScopes: AccessDataScopeSetting[];
  roleId: number;
}

export interface AccessRoleMember {
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

export interface AccessRoleMemberQuery {
  keywords?: string;
  pageNum: number;
  pageSize: number;
}

export interface AccessMenuRecord {
  apiPerms?: null | string;
  cacheFlag: boolean;
  component?: null | string;
  contextMenuId?: null | number;
  createTime?: null | string;
  createUserId?: null | number;
  disabledFlag: boolean;
  frameFlag: boolean;
  frameUrl?: null | string;
  icon?: null | string;
  menuId: number;
  menuName: string;
  menuType: number;
  parentId: number;
  path?: null | string;
  permsType?: null | number;
  routeId?: null | string;
  sort?: null | number;
  updateTime?: null | string;
  updateUserId?: null | number;
  visibleFlag: boolean;
  webPerms?: null | string;
}

export interface AccessMenuTreeRecord extends AccessMenuRecord {
  children?: AccessMenuTreeRecord[];
}

export interface AccessMenuCommand {
  apiPerms?: null | string;
  cacheFlag: boolean;
  component?: null | string;
  contextMenuId?: null | number;
  disabledFlag: boolean;
  frameFlag: boolean;
  frameUrl?: null | string;
  icon?: null | string;
  menuName: string;
  menuType: number;
  parentId: number;
  path?: null | string;
  permsType?: null | number;
  routeId?: null | string;
  sort?: null | number;
  visibleFlag: boolean;
  webPerms?: null | string;
}

export interface AccessMenuUpdateCommand extends AccessMenuCommand {
  menuId: number;
}

export interface AccessAuthorizationUrl {
  comment?: null | string;
  name?: null | string;
  url?: null | string;
}

export interface AccessClient {
  assignRoleMembers(roleId: number, employeeIds: number[]): Promise<void>;
  createMenu(command: AccessMenuCommand): Promise<number>;
  createRole(command: AccessRoleCommand): Promise<number>;
  deleteMenus(menuIds: number[]): Promise<void>;
  deleteRole(roleId: number): Promise<void>;
  getMenu(menuId: number): Promise<AccessMenuRecord>;
  getRole(roleId: number): Promise<AccessRoleRecord>;
  getRoleCapabilities(roleId: number): Promise<AccessRoleCapabilityGrant>;
  getRoleDataScopes(roleId: number): Promise<AccessRoleDataScopes>;
  listAuthorizationUrls(): Promise<AccessAuthorizationUrl[]>;
  listDataScopes(): Promise<AccessDataScopeDefinition[]>;
  listMenus(): Promise<AccessMenuRecord[]>;
  listMenuTree(onlyMenu: boolean): Promise<AccessMenuTreeRecord[]>;
  listRoleMembers(roleId: number): Promise<AccessRoleMember[]>;
  listRoles(): Promise<AccessRoleRecord[]>;
  queryRoleMemberCandidates(
    roleId: number,
    query: AccessRoleMemberQuery,
  ): Promise<PageResult<AccessRoleMember>>;
  queryRoleMembers(
    roleId: number,
    query: AccessRoleMemberQuery,
  ): Promise<PageResult<AccessRoleMember>>;
  removeRoleMembers(roleId: number, employeeIds: number[]): Promise<void>;
  replaceRoleCapabilities(
    roleId: number,
    capabilityIds: number[],
  ): Promise<void>;
  replaceRoleDataScopes(
    roleId: number,
    dataScopes: AccessDataScopeSetting[],
  ): Promise<void>;
  updateMenu(command: AccessMenuUpdateCommand): Promise<void>;
  updateRole(roleId: number, command: AccessRoleCommand): Promise<void>;
}
