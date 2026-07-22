import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const positionPagePath =
  'apps/hunyuan-system/src/views/system/position/position-list.vue';
const roleEntryPath = 'apps/hunyuan-system/src/views/system/role/index.vue';
const menuEntryPath = 'apps/hunyuan-system/src/views/system/menu/menu-list.vue';
const roleFeaturePath = 'packages/features/access/src/role/index.vue';
const menuFeaturePath = 'packages/features/access/src/menu/index.vue';
const accessClientPath = 'packages/features/access/src/client.ts';
const accessIndexPath = 'packages/features/access/src/index.ts';
const artOrgTreePath =
  'packages/@vben/art-hooks/src/tree/components/art-org-tree/tree.vue';
const systemIndexPath = 'apps/hunyuan-system/index.html';
const systemFaviconPath = 'apps/hunyuan-system/public/favicon.svg';
const menuApiPath = 'apps/hunyuan-system/src/api/system/menu.ts';
const legacyDepartmentPagePath =
  'apps/hunyuan-system/src/views/system/department/department-list.vue';
const organizationApiPath =
  'apps/hunyuan-system/src/api/system/organization.ts';
const legacyEmployeeComponentsPath =
  'apps/hunyuan-system/src/views/system/employee/components';

const legacyAccessFragments = [
  '/role/add',
  '/role/update',
  '/role/delete/',
  '/role/get/',
  '/role/getAll',
  '/role/menu/',
  '/role/dataScope/',
  '/role/employee/',
  '/menu/query',
  '/menu/tree',
  '/menu/add',
  '/menu/update',
  '/menu/delete',
  '/menu/auth/url',
  '/dataScope/list',
] as const;

const actionPages = [
  {
    actionClass: 'employee-table-panel__actions',
    label: 'employee',
    path: 'packages/features/identity-employee/src/employee/components/employee-table-panel.vue',
  },
  {
    actionClass: 'position-page__actions',
    label: 'position',
    path: positionPagePath,
  },
] as const;

function readWorkspaceFile(path: string) {
  return readFileSync(resolve(process.cwd(), path), 'utf8');
}

describe('组织与访问控制前端边界', () => {
  it('退役旧部门页面和员工管理客户端', () => {
    expect(existsSync(resolve(process.cwd(), legacyDepartmentPagePath))).toBe(
      false,
    );

    const legacyComponentsDirectory = resolve(
      process.cwd(),
      legacyEmployeeComponentsPath,
    );
    expect(
      existsSync(legacyComponentsDirectory)
        ? readdirSync(legacyComponentsDirectory)
        : [],
    ).toEqual([]);

    const source = readWorkspaceFile(organizationApiPath);
    expect(source).not.toContain('/department/');
    expect(source).not.toContain("'/employee/query'");
    expect(source).not.toContain("'/employee/add'");
    expect(source).not.toContain("'/employee/update'");
    expect(source).not.toContain('/employee/update/disabled/');
    expect(source).not.toContain('/employee/update/batch/');
  });

  it('组织兼容 API 只保留岗位能力', () => {
    const source = readWorkspaceFile(organizationApiPath);

    expect(source).toContain("'/position/queryList'");
    expect(source).toContain("'/position/queryPage'");
    expect(source).toContain("'/position/add'");
    expect(source).toContain("'/position/update'");
    expect(source).not.toContain('/role/');
    expect(source).not.toContain('/menu/');
    expect(source).not.toContain('/dataScope/');
  });

  it('角色和菜单应用页面保持为 feature 薄入口', () => {
    const roleEntry = readWorkspaceFile(roleEntryPath);
    const menuEntry = readWorkspaceFile(menuEntryPath);

    expect(roleEntry).toContain("from '@hunyuan/feature-access'");
    expect(roleEntry).toContain("from '@hunyuan/feature-access/role-page'");
    expect(roleEntry).toContain(
      'provide(accessClientKey, createAccessClient(requestClient));',
    );
    expect(roleEntry).toContain('<AccessRolePage />');
    expect(roleEntry).not.toContain('role-page__permission-matrix');

    expect(menuEntry).toContain("from '@hunyuan/feature-access'");
    expect(menuEntry).toContain("from '@hunyuan/feature-access/menu-page'");
    expect(menuEntry).toContain(
      'provide(accessClientKey, createAccessClient(requestClient));',
    );
    expect(menuEntry).toContain('<AccessMenuPage />');
    expect(menuEntry).not.toContain('menu-page__actions');
  });

  it('access feature 拥有角色权限、数据范围和员工管理页面', () => {
    const source = readWorkspaceFile(roleFeaturePath);

    expect(source).toContain('role-page__permission-matrix');
    expect(source).toContain('accessClient.getRoleCapabilities');
    expect(source).toContain('accessClient.replaceRoleCapabilities');
    expect(source).toContain('accessClient.listDataScopes');
    expect(source).toContain('accessClient.getRoleDataScopes');
    expect(source).toContain('accessClient.replaceRoleDataScopes');
    expect(source).toContain('accessClient.queryRoleMembers');
    expect(source).toContain('accessClient.queryRoleMemberCandidates');
    expect(source).toContain('accessClient.assignRoleMembers');
    expect(source).toContain('accessClient.removeRoleMembers');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).not.toContain('ElTable');
    expect(source).toContain(':value="viewType.viewType"');
    expect(source).not.toContain(':label="viewType.viewType"');
  });

  it('角色页面保持密集布局并使用稳定能力码控制操作', () => {
    const source = readWorkspaceFile(roleFeaturePath);

    expect(source).not.toContain('role-page__title');
    expect(source).not.toContain('role-page__hero');
    expect(source).not.toContain('role-page__desc');
    expect(source).not.toContain('ArtSearchPanel');
    expect(source).not.toContain('role-page__keyword-item');
    expect(source).toContain('<ElIcon class="role-page__role-icon">');
    expect(source).toContain('role-page__role-item--active');
    expect(source).toContain('grid-template-columns: 248px minmax(0, 1fr);');
    expect(source).toContain("['access.role.create']");
    expect(source).toContain("'access.capability.grant'");
    expect(source).toContain("'access.data-scope.update'");
    expect(source).toContain("['access.role.employee.assign']");
    expect(source).toContain("['access.role.employee.remove']");
  });

  it('access feature 拥有完整菜单管理页面和稳定能力控制', () => {
    const source = readWorkspaceFile(menuFeaturePath);

    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('row-key="menuId"');
    expect(source).toContain(':tree-props="{ children: \'children\' }"');
    expect(source).not.toContain('menu-page__title');
    expect(source).not.toContain('menu-page__hero');
    expect(source).not.toContain('menu-page__desc');
    expect(source).toContain(':collapsible="false"');
    expect(source).toContain("['access.menu.create']");
    expect(source).toContain("['access.menu.update']");
    expect(source).toContain("['access.menu.delete']");
  });

  it('菜单页面保留原有字段、层级保护和紧凑操作区', () => {
    const source = readWorkspaceFile(menuFeaturePath);

    expect(source).toContain('path');
    expect(source).toContain('component');
    expect(source).toContain('/system/menu/menu-list.vue');
    expect(source).toContain('webPerms');
    expect(source).toContain('apiPerms');
    expect(source).toContain('frameFlag');
    expect(source).toContain('visibleFlag');
    expect(source).toContain('disabledFlag');
    expect(source).toContain('class="menu-page__actions"');
    expect(source).toContain('.menu-page__actions {');
    expect(source).toContain('display: inline-flex;');
    expect(source).toContain('gap: 8px;');
    expect(source).toContain('.menu-page__actions :deep(.el-button)');
    expect(source).toContain('font-size: 14px;');
    expect(source).toContain('line-height: 22px;');
    expect(source).toContain('padding: 0;');
    expect(source).toContain(
      '.menu-page__actions :deep(.el-button + .el-button)',
    );
    expect(source).toContain('margin-left: 0;');
    expect(source).toContain('function openAddChildDialog(row: MenuTreeRow) {');
    expect(source).toContain('if (row.menuType === 3) {');
    expect(source).toContain('ElMessage.warning');
    expect(source).toContain('v-if="row.menuType !== 3"');
    expect(source).toContain('function collectDescendantMenuIds(');
    expect(source).toContain('const disabledParentMenuIds = computed(() =>');
    expect(source).toContain('collectDescendantMenuIds(formData.menuId');
    expect(source).toContain(
      ':disabled="disabledParentMenuIds.has(item.menuId)"',
    );
  });

  it('access 客户端只使用稳定版本化路径并退役旧菜单 API 文件', () => {
    expect(existsSync(resolve(process.cwd(), menuApiPath))).toBe(false);

    const sources = [
      readWorkspaceFile(accessClientPath),
      readWorkspaceFile(roleFeaturePath),
      readWorkspaceFile(menuFeaturePath),
      readWorkspaceFile(roleEntryPath),
      readWorkspaceFile(menuEntryPath),
    ];

    expect(sources[0]).toContain("const BASE_PATH = '/admin/v1/access';");
    for (const source of sources) {
      for (const fragment of legacyAccessFragments) {
        expect(source).not.toContain(fragment);
      }
    }
  });

  it('冻结十五个稳定能力码和两个应用路由', () => {
    const source = readWorkspaceFile(accessIndexPath);
    const capabilityMatches =
      source
        .match(/'access\.[^']+'/g)
        ?.filter((item) => item !== "'access.management'") ?? [];

    expect(new Set(capabilityMatches).size).toBe(15);
    expect(source).toContain("id: 'access.management'");
    expect(source).toContain("path: '/system/role'");
    expect(source).toContain("component: '/system/role/index.vue'");
    expect(source).toContain("path: '/system/menu'");
    expect(source).toContain("component: '/system/menu/menu-list.vue'");
  });

  it('共享组织树注册 Element Plus 输入组件', () => {
    const source = readWorkspaceFile(artOrgTreePath);

    expect(source).toContain("import { ElInput, ElTree } from 'element-plus';");
  });

  it('系统应用声明本地 favicon', () => {
    const source = readWorkspaceFile(systemIndexPath);

    expect(source).toContain('<link rel="icon" href="/favicon.svg" />');
    expect(existsSync(resolve(process.cwd(), systemFaviconPath))).toBe(true);
  });

  it('岗位页面保持真实表格与密集布局', () => {
    const source = readWorkspaceFile(positionPagePath);

    expect(source).toContain('ArtTable');
    expect(source).not.toContain('position-page__title');
    expect(source).not.toContain('position-page__hero');
    expect(source).not.toContain('position-page__desc');
    expect(source).toContain(':collapsible="false"');
    expect(source).toContain('columnsFactory');
    expect(source).toContain('columnChecks');
    expect(source).toContain('v-model="columnChecks"');
    expect(source).not.toContain(':model-value="[]"');
    expect(source).toContain(
      'layout="search,size,fullscreen,columns,settings"',
    );
    expect(source).toContain('showSearchBar');
    expect(source).toContain('@search="handleToggleSearchBar"');
  });

  it.each(actionPages)(
    '$label 行操作保持紧凑且可度量',
    ({ actionClass, path }) => {
      const source = readWorkspaceFile(path);

      expect(source).toContain(`class="${actionClass}"`);
      expect(source).toContain(`.${actionClass} {`);
      expect(source).toContain('display: inline-flex;');
      expect(source).toContain('gap: 8px;');
      expect(source).toContain(`.${actionClass} :deep(.el-button)`);
      expect(source).toContain('font-size: 14px;');
      expect(source).toContain('line-height: 22px;');
      expect(source).toContain('padding: 0;');
      expect(source).toContain(
        `.${actionClass} :deep(.el-button + .el-button)`,
      );
      expect(source).toContain('margin-left: 0;');
    },
  );
});
