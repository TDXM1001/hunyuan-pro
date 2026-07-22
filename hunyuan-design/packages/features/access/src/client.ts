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

function cleanText(value?: null | string) {
  return value?.trim() || '';
}

export function buildRolePayload(
  command: AccessRoleCommand,
): AccessRoleCommand {
  return {
    remark: cleanText(command.remark),
    roleCode: command.roleCode.trim(),
    roleName: command.roleName.trim(),
  };
}

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

export function buildMemberQueryPayload(query: AccessRoleMemberQuery) {
  return {
    keywords: query.keywords?.trim() || undefined,
    pageNum: query.pageNum,
    pageSize: query.pageSize,
  };
}

export function buildEmployeeIdsPayload(employeeIds: number[]) {
  return { employeeIds: [...new Set(employeeIds)] };
}

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
