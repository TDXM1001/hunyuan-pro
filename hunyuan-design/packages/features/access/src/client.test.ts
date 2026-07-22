import type { RequestClient } from '@vben/request';

import { describe, expect, it, vi } from 'vitest';

import {
  buildEmployeeIdsPayload,
  buildMemberQueryPayload,
  buildMenuPayload,
  buildRolePayload,
  createAccessClient,
} from './client';
import { accessFeature } from './index';

function createRequestMock() {
  return {
    delete: vi.fn().mockResolvedValue(undefined),
    get: vi.fn().mockResolvedValue([]),
    post: vi.fn().mockResolvedValue(101),
    put: vi.fn().mockResolvedValue(undefined),
  } as unknown as RequestClient;
}

describe('access feature 客户端契约', () => {
  it('统一使用版本化访问控制查询路径', async () => {
    const request = createRequestMock();
    const client = createAccessClient(request);

    await client.listRoles();
    await client.getRole(7);
    await client.getRoleCapabilities(7);
    await client.listDataScopes();
    await client.getRoleDataScopes(7);
    await client.listRoleMembers(7);
    await client.listMenus();
    await client.getMenu(9);
    await client.listMenuTree(true);
    await client.listAuthorizationUrls();

    expect(request.get).toHaveBeenNthCalledWith(1, '/admin/v1/access/roles');
    expect(request.get).toHaveBeenNthCalledWith(2, '/admin/v1/access/roles/7');
    expect(request.get).toHaveBeenNthCalledWith(
      3,
      '/admin/v1/access/roles/7/capabilities',
    );
    expect(request.get).toHaveBeenNthCalledWith(
      4,
      '/admin/v1/access/data-scopes',
    );
    expect(request.get).toHaveBeenNthCalledWith(
      5,
      '/admin/v1/access/roles/7/data-scopes',
    );
    expect(request.get).toHaveBeenNthCalledWith(
      6,
      '/admin/v1/access/roles/7/members',
    );
    expect(request.get).toHaveBeenNthCalledWith(7, '/admin/v1/access/menus');
    expect(request.get).toHaveBeenNthCalledWith(8, '/admin/v1/access/menus/9');
    expect(request.get).toHaveBeenNthCalledWith(
      9,
      '/admin/v1/access/menus/tree',
      { params: { onlyMenu: true } },
    );
    expect(request.get).toHaveBeenNthCalledWith(
      10,
      '/admin/v1/access/menus/authorization-urls',
    );
  });

  it('角色生命周期使用稳定方法并裁剪文本字段', async () => {
    const request = createRequestMock();
    const client = createAccessClient(request);
    const command = {
      remark: ' 平台管理员 ',
      roleCode: ' platform_admin ',
      roleName: ' 平台管理员 ',
    };

    await client.createRole(command);
    await client.updateRole(7, command);
    await client.deleteRole(7);

    const expected = {
      remark: '平台管理员',
      roleCode: 'platform_admin',
      roleName: '平台管理员',
    };
    expect(request.post).toHaveBeenCalledWith(
      '/admin/v1/access/roles',
      expected,
    );
    expect(request.put).toHaveBeenCalledWith(
      '/admin/v1/access/roles/7',
      expected,
    );
    expect(request.delete).toHaveBeenCalledWith('/admin/v1/access/roles/7');
    expect(buildRolePayload(command)).toEqual(expected);
  });

  it('角色能力、数据范围和成员写操作使用全量替换与 DELETE body', async () => {
    const request = createRequestMock();
    const client = createAccessClient(request);

    await client.replaceRoleCapabilities(7, [3, 2, 3]);
    await client.replaceRoleDataScopes(7, [{ dataScopeType: 1, viewType: 10 }]);
    await client.assignRoleMembers(7, [9, 8, 9]);
    await client.removeRoleMembers(7, [9, 8, 9]);

    expect(request.put).toHaveBeenNthCalledWith(
      1,
      '/admin/v1/access/roles/7/capabilities',
      { capabilityIds: [3, 2] },
    );
    expect(request.put).toHaveBeenNthCalledWith(
      2,
      '/admin/v1/access/roles/7/data-scopes',
      { dataScopes: [{ dataScopeType: 1, viewType: 10 }] },
    );
    expect(request.post).toHaveBeenCalledWith(
      '/admin/v1/access/roles/7/members',
      { employeeIds: [9, 8] },
    );
    expect(request.delete).toHaveBeenCalledWith(
      '/admin/v1/access/roles/7/members',
      { data: { employeeIds: [9, 8] } },
    );
    expect(buildEmployeeIdsPayload([9, 8, 9])).toEqual({
      employeeIds: [9, 8],
    });
  });

  it('角色成员查询使用 POST 参数体并裁剪关键词', async () => {
    const request = createRequestMock();
    const client = createAccessClient(request);
    const query = {
      keywords: ' 管理员 ',
      pageNum: 2,
      pageSize: 20,
    };

    await client.queryRoleMembers(7, query);
    await client.queryRoleMemberCandidates(7, query);

    const expected = {
      keywords: '管理员',
      pageNum: 2,
      pageSize: 20,
    };
    expect(request.post).toHaveBeenNthCalledWith(
      1,
      '/admin/v1/access/roles/7/members/query',
      expected,
    );
    expect(request.post).toHaveBeenNthCalledWith(
      2,
      '/admin/v1/access/roles/7/member-candidates/query',
      expected,
    );
    expect(buildMemberQueryPayload({ ...query, keywords: '   ' })).toEqual({
      keywords: undefined,
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('菜单生命周期规范化请求体并通过 DELETE body 批量删除', async () => {
    const request = createRequestMock();
    const client = createAccessClient(request);
    const command = {
      apiPerms: ' access.menu.read ',
      cacheFlag: true,
      component: ' /system/menu/menu-list.vue ',
      contextMenuId: undefined,
      disabledFlag: false,
      frameFlag: false,
      frameUrl: ' ',
      icon: ' menu ',
      menuName: ' 菜单管理 ',
      menuType: 2,
      parentId: undefined,
      path: ' /system/menu ',
      permsType: undefined,
      sort: undefined,
      visibleFlag: true,
      webPerms: ' access.menu.read ',
    };

    await client.createMenu(command);
    await client.updateMenu({ ...command, menuId: 9 });
    await client.deleteMenus([9, 8, 9]);

    const expected = {
      apiPerms: 'access.menu.read',
      cacheFlag: true,
      component: '/system/menu/menu-list.vue',
      contextMenuId: null,
      disabledFlag: false,
      frameFlag: false,
      frameUrl: '',
      icon: 'menu',
      menuName: '菜单管理',
      menuType: 2,
      parentId: 0,
      path: '/system/menu',
      permsType: null,
      sort: 0,
      visibleFlag: true,
      webPerms: 'access.menu.read',
    };
    expect(request.post).toHaveBeenCalledWith(
      '/admin/v1/access/menus',
      expected,
    );
    expect(request.put).toHaveBeenCalledWith(
      '/admin/v1/access/menus/9',
      expected,
    );
    expect(request.delete).toHaveBeenCalledWith('/admin/v1/access/menus', {
      data: { menuIds: [9, 8] },
    });
    expect(buildMenuPayload(command)).toEqual(expected);
  });

  it('冻结两个应用路由和十五个稳定能力码', () => {
    expect(accessFeature.id).toBe('access.management');
    expect(accessFeature.routes).toEqual([
      {
        component: '/system/role/index.vue',
        path: '/system/role',
      },
      {
        component: '/system/menu/menu-list.vue',
        path: '/system/menu',
      },
    ]);
    expect(accessFeature.capabilities).toEqual([
      'access.role.read',
      'access.role.create',
      'access.role.update',
      'access.role.delete',
      'access.role.employee.read',
      'access.role.employee.assign',
      'access.role.employee.remove',
      'access.capability.read',
      'access.capability.grant',
      'access.menu.read',
      'access.menu.create',
      'access.menu.update',
      'access.menu.delete',
      'access.data-scope.read',
      'access.data-scope.update',
    ]);
    expect(new Set(accessFeature.capabilities).size).toBe(15);
  });
});
