/**
 * 访问控制 feature 的前后端数据约定。
 *
 * 这里的类型只描述“数据长什么样”，不负责发请求或控制页面。页面、请求客户端和测试都依赖它，
 * 因此字段名必须和后端接口保持一致。带 ? 的字段表示后端在部分历史数据中可能不返回，null 表示后端明确返回空值。
 */
export interface PageResult<T> {
  /** 后端用于标记当前结果是否为空，页面通常直接根据 list 判断。 */
  emptyFlag?: boolean;
  /** 当前页的数据记录。 */
  list: T[];
  /** 当前页码和每页数量，便于分页器恢复服务端状态。 */
  pageNum: number;
  pages: number;
  pageSize: number;
  total: number;
}

export interface AccessRoleRecord {
  /** 角色列表中展示的说明、编码、主键和名称。 */
  remark?: null | string;
  roleCode: string;
  roleId: number;
  roleName: string;
}

export interface AccessRoleCommand {
  /** 新增或修改角色时提交的字段，不包含由后端生成的 roleId。 */
  remark?: null | string;
  roleCode: string;
  roleName: string;
}

export interface AccessCapabilityNode {
  /** 权限树节点；children 用来表达目录、菜单和功能点的父子关系。 */
  capabilityId: number;
  capabilityName: string;
  capabilityType: number;
  children?: AccessCapabilityNode[];
  contextCapabilityId?: null | number;
  parentId?: null | number;
}

export interface AccessRoleCapabilityGrant {
  /** 某个角色当前能看到的权限树，以及已经选中的权限 ID。 */
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
  /** 一个数据范围类型及其可选查看级别，例如本人、本部门或全部数据。 */
  dataScopeType: number;
  dataScopeTypeDescription: string;
  dataScopeTypeName: string;
  sort: number;
  viewOptions: AccessDataScopeViewOption[];
}

export interface AccessDataScopeSetting {
  /** 角色在一种数据范围类型上最终选择的查看级别。 */
  dataScopeType: number;
  viewType: number;
}

export interface AccessRoleDataScopes {
  dataScopes: AccessDataScopeSetting[];
  roleId: number;
}

export interface AccessRoleMember {
  /** 角色成员和候选成员共用的员工展示模型。 */
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
  /** 菜单管理页面使用的完整菜单记录，包含路由、权限、缓存和显示状态。 */
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
  /** 新增或修改菜单时提交的可编辑字段；menuId 在更新命令中单独补充。 */
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
  /**
   * 访问控制客户端的业务动作清单。
   * 页面只调用这些有业务含义的方法，不直接拼接 URL，这样路径和请求体的兼容处理集中在 client.ts。
   */
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
