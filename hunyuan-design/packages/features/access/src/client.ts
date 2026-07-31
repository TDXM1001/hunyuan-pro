import type { RequestClient } from '@vben/request';

import type {
  AccessAuthorizationUrl,
  AccessClient,
  AccessDataScopeDefinition,
  AccessDataScopeSetting,
  AccessMenuCommand,
  AccessMenuRecord,
  AccessMenuTreeRecord,
  AccessMenuUpdateCommand,
  AccessRoleCapabilityGrant,
  AccessRoleCommand,
  AccessRoleDataScopes,
  AccessRoleMember,
  AccessRoleMemberQuery,
  AccessRoleRecord,
  PageResult,
} from './contract';

const BASE_PATH = '/admin/v1/access';

// 访问控制接口允许空文本进入后端，但空白字符串没有业务含义；统一归一化后再组装请求体。
function cleanText(value?: null | string) {
  return value?.trim() || '';
}

/** 将角色表单中的文本清理干净，再交给后端保存。 */
export function buildRolePayload(
  command: AccessRoleCommand,
): AccessRoleCommand {
  return {
    remark: cleanText(command.remark),
    roleCode: command.roleCode.trim(),
    roleName: command.roleName.trim(),
  };
}

/**
 * 组装菜单新增/更新请求体。
 * 菜单接口对空值有明确约定：根节点 parentId 使用 0，未填写的数字选项使用默认值，文本则去掉首尾空格。
 */
export function buildMenuPayload<T extends AccessMenuCommand>(command: T): T {
  return {
    ...command,
    apiPerms: cleanText(command.apiPerms),
    component: cleanText(command.component),
    contextMenuId: command.contextMenuId ?? null,
    frameUrl: cleanText(command.frameUrl),
    icon: cleanText(command.icon),
    menuName: command.menuName.trim(),
    parentId: command.parentId ?? 0,
    path: cleanText(command.path),
    permsType: command.permsType ?? null,
    sort: command.sort ?? 0,
    webPerms: cleanText(command.webPerms),
  };
}

/** 将成员查询关键词转换成后端分页查询需要的格式，空关键词不发送。 */
export function buildMemberQueryPayload(query: AccessRoleMemberQuery) {
  return {
    keywords: query.keywords?.trim() || undefined,
    pageNum: query.pageNum,
    pageSize: query.pageSize,
  };
}

/** 将页面选中的员工 ID 转为集合，避免重复选择导致重复授权。 */
export function buildEmployeeIdsPayload(employeeIds: number[]) {
  // 成员授权接口按集合语义处理，前端先去重，避免重复提交造成审计记录和结果不一致。
  return { employeeIds: [...new Set(employeeIds)] };
}

/**
 * 创建访问控制客户端。
 * requestClient 由应用外壳注入，feature 本身不关心登录态、Token 或代理地址，只负责调用稳定业务路径。
 */
export function createAccessClient(requestClient: RequestClient): AccessClient {
  return {
    async assignRoleMembers(roleId, employeeIds) {
      await requestClient.post(
        `${BASE_PATH}/roles/${roleId}/members`,
        buildEmployeeIdsPayload(employeeIds),
      );
    },
    createMenu(command) {
      return requestClient.post<number>(
        `${BASE_PATH}/menus`,
        buildMenuPayload(command),
      );
    },
    createRole(command) {
      return requestClient.post<number>(
        `${BASE_PATH}/roles`,
        buildRolePayload(command),
      );
    },
    async deleteMenus(menuIds) {
      await requestClient.delete(`${BASE_PATH}/menus`, {
        data: { menuIds: [...new Set(menuIds)] },
      });
    },
    async deleteRole(roleId) {
      await requestClient.delete(`${BASE_PATH}/roles/${roleId}`);
    },
    getMenu(menuId) {
      return requestClient.get<AccessMenuRecord>(
        `${BASE_PATH}/menus/${menuId}`,
      );
    },
    getRole(roleId) {
      return requestClient.get<AccessRoleRecord>(
        `${BASE_PATH}/roles/${roleId}`,
      );
    },
    getRoleCapabilities(roleId) {
      return requestClient.get<AccessRoleCapabilityGrant>(
        `${BASE_PATH}/roles/${roleId}/capabilities`,
      );
    },
    getRoleDataScopes(roleId) {
      return requestClient.get<AccessRoleDataScopes>(
        `${BASE_PATH}/roles/${roleId}/data-scopes`,
      );
    },
    listAuthorizationUrls() {
      return requestClient.get<AccessAuthorizationUrl[]>(
        `${BASE_PATH}/menus/authorization-urls`,
      );
    },
    listDataScopes() {
      return requestClient.get<AccessDataScopeDefinition[]>(
        `${BASE_PATH}/data-scopes`,
      );
    },
    listMenus() {
      return requestClient.get<AccessMenuRecord[]>(`${BASE_PATH}/menus`);
    },
    listMenuTree(onlyMenu) {
      return requestClient.get<AccessMenuTreeRecord[]>(
        `${BASE_PATH}/menus/tree`,
        {
          params: { onlyMenu },
        },
      );
    },
    listRoleMembers(roleId) {
      return requestClient.get<AccessRoleMember[]>(
        `${BASE_PATH}/roles/${roleId}/members`,
      );
    },
    listRoles() {
      return requestClient.get<AccessRoleRecord[]>(`${BASE_PATH}/roles`);
    },
    queryRoleMemberCandidates(roleId, query) {
      return requestClient.post<PageResult<AccessRoleMember>>(
        `${BASE_PATH}/roles/${roleId}/member-candidates/query`,
        buildMemberQueryPayload(query),
      );
    },
    queryRoleMembers(roleId, query) {
      return requestClient.post<PageResult<AccessRoleMember>>(
        `${BASE_PATH}/roles/${roleId}/members/query`,
        buildMemberQueryPayload(query),
      );
    },
    async removeRoleMembers(roleId, employeeIds) {
      await requestClient.delete(`${BASE_PATH}/roles/${roleId}/members`, {
        data: buildEmployeeIdsPayload(employeeIds),
      });
    },
    async replaceRoleCapabilities(roleId, capabilityIds) {
      // 能力授权是全量替换契约，不能按增量理解，否则取消勾选的能力会残留。
      await requestClient.put(`${BASE_PATH}/roles/${roleId}/capabilities`, {
        capabilityIds: [...new Set(capabilityIds)],
      });
    },
    async replaceRoleDataScopes(roleId, dataScopes: AccessDataScopeSetting[]) {
      await requestClient.put(`${BASE_PATH}/roles/${roleId}/data-scopes`, {
        dataScopes: dataScopes.map((item) => ({ ...item })),
      });
    },
    async updateMenu(command: AccessMenuUpdateCommand) {
      const { menuId, ...payload } = buildMenuPayload(command);
      await requestClient.put(`${BASE_PATH}/menus/${menuId}`, payload);
    },
    async updateRole(roleId, command) {
      await requestClient.put(
        `${BASE_PATH}/roles/${roleId}`,
        buildRolePayload(command),
      );
    },
  };
}
