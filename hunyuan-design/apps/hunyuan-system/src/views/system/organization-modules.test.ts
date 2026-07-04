import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const modulePages = [
  {
    label: 'department',
    path: 'apps/hunyuan-system/src/views/system/department/department-list.vue',
  },
  {
    label: 'position',
    path: 'apps/hunyuan-system/src/views/system/position/position-list.vue',
  },
] as const;

const rolePagePath = 'apps/hunyuan-system/src/views/system/role/index.vue';
const artOrgTreePath =
  'packages/@vben/art-hooks/src/tree/components/art-org-tree/tree.vue';
const systemIndexPath = 'apps/hunyuan-system/index.html';
const systemFaviconPath = 'apps/hunyuan-system/public/favicon.svg';
const menuPagePath = 'apps/hunyuan-system/src/views/system/menu/index.vue';
const menuApiPath = 'apps/hunyuan-system/src/api/system/menu.ts';

const actionPages = [
  {
    actionClass: 'employee-table-panel__actions',
    label: 'employee',
    path: 'apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue',
  },
  {
    actionClass: 'department-page__actions',
    label: 'department',
    path: 'apps/hunyuan-system/src/views/system/department/department-list.vue',
  },
  {
    actionClass: 'position-page__actions',
    label: 'position',
    path: 'apps/hunyuan-system/src/views/system/position/position-list.vue',
  },
] as const;

describe('organization backend menu docking pages', () => {
  it.each(modulePages)('provides a real $label page', ({ path }) => {
    const pagePath = resolve(process.cwd(), path);

    expect(existsSync(pagePath)).toBe(true);
    expect(readFileSync(pagePath, 'utf8')).toContain('ArtTable');
  });

  it('provides a real role permission workspace page', () => {
    const pagePath = resolve(process.cwd(), rolePagePath);

    expect(existsSync(pagePath)).toBe(true);
    expect(readFileSync(pagePath, 'utf8')).toContain(
      'role-page__permission-matrix',
    );
  });

  it('keeps the role page dense without extra title, explainer block, or collapse toggle', () => {
    const pagePath = resolve(
      process.cwd(),
      rolePagePath,
    );
    const source = readFileSync(pagePath, 'utf8');

    expect(source).not.toContain('role-page__title');
    expect(source).not.toContain('role-page__hero');
    expect(source).not.toContain('role-page__desc');
    expect(source).not.toContain('ArtSearchPanel');
    expect(source).not.toContain('role-page__keyword-item');
    expect(source).toContain('<ElIcon class="role-page__role-icon">');
    expect(source).toContain('role-page__role-item--active');
    expect(source).toContain('grid-template-columns: 248px minmax(0, 1fr);');
  });

  it('renders role permissions as a backend-backed matrix', () => {
    const pagePath = resolve(
      process.cwd(),
      rolePagePath,
    );
    const source = readFileSync(pagePath, 'utf8');

    expect(source).toContain('role-page__permission-matrix');
    expect(source).toContain('getRoleSelectedMenu');
    expect(source).toContain('updateRoleMenu');
    expect(source).toContain('selectedMenuIds');
  });

  it('wires role data scope and employee tabs to backend APIs', () => {
    const pagePath = resolve(process.cwd(), rolePagePath);
    const source = readFileSync(pagePath, 'utf8');

    expect(source).toContain('listDataScopes');
    expect(source).toContain('getRoleDataScopeList');
    expect(source).toContain('updateRoleDataScopeList');
    expect(source).toContain('queryRoleEmployees');
    expect(source).toContain('queryCandidateRoleEmployees');
    expect(source).toContain('batchAddRoleEmployees');
    expect(source).toContain('batchRemoveRoleEmployees');
    expect(source).not.toContain('queryEmployeePage');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).not.toContain('ElTable');
    expect(source).not.toContain('数据范围配置接口已存在，下一步接入');
    expect(source).not.toContain('员工列表接口已存在，下一步接入');
  });

  it('uses the Element Plus 3 compatible value prop on role data scope radios', () => {
    const pagePath = resolve(process.cwd(), rolePagePath);
    const source = readFileSync(pagePath, 'utf8');

    expect(source).toContain(':value="viewType.viewType"');
    expect(source).not.toContain(':label="viewType.viewType"');
  });

  it('provides a real menu management page', () => {
    const pagePath = resolve(process.cwd(), menuPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('row-key="menuId"');
    expect(source).toContain(':tree-props="{ children: \'children\' }"');
  });

  it('keeps menu management dense without extra page title or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).not.toContain('menu-page__title');
    expect(source).not.toContain('menu-page__hero');
    expect(source).not.toContain('menu-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires menu management to backend menu endpoints', () => {
    const apiPath = resolve(process.cwd(), menuApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/menu/query'");
    expect(source).toContain("'/menu/tree'");
    expect(source).toContain("'/menu/add'");
    expect(source).toContain("'/menu/update'");
    expect(source).toContain(
      "buildRepeatedQueryString('menuIdList', menuIdList)",
    );
    expect(source).toContain("'/menu/auth/url'");
    expect(source).toContain('comment?: null | string;');
    expect(source).not.toContain('method?: null | string;');
  });

  it('surfaces route, component, and permission fields on the menu page', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).toContain('path');
    expect(source).toContain('component');
    expect(source).toContain('webPerms');
    expect(source).toContain('apiPerms');
    expect(source).toContain('frameFlag');
    expect(source).toContain('visibleFlag');
    expect(source).toContain('disabledFlag');
  });

  it('keeps menu row actions compact and measurable', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).toContain('class="menu-page__actions"');
    expect(source).toContain('.menu-page__actions {');
    expect(source).toContain('display: inline-flex;');
    expect(source).toContain('gap: 8px;');
    expect(source).toContain('.menu-page__actions :deep(.el-button)');
    expect(source).toContain('font-size: 14px;');
    expect(source).toContain('line-height: 22px;');
    expect(source).toContain('padding: 0;');
    expect(source).toContain('.menu-page__actions :deep(.el-button + .el-button)');
    expect(source).toContain('margin-left: 0;');
  });

  it('registers Element Plus inputs used by the shared organization tree', () => {
    const pagePath = resolve(process.cwd(), artOrgTreePath);
    const source = readFileSync(pagePath, 'utf8');

    expect(source).toContain('import { ElInput, ElTree } from \'element-plus\';');
  });

  it('declares a local favicon asset for the hunyuan system app shell', () => {
    const indexPath = resolve(process.cwd(), systemIndexPath);
    const faviconPath = resolve(process.cwd(), systemFaviconPath);
    const source = readFileSync(indexPath, 'utf8');

    expect(source).toContain('<link rel="icon" href="/favicon.svg" />');
    expect(existsSync(faviconPath)).toBe(true);
  });

  it('keeps department and position pages without extra title or explainer copy', () => {
    const departmentSource = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/department/department-list.vue',
      ),
      'utf8',
    );
    const positionSource = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/position/position-list.vue',
      ),
      'utf8',
    );

    expect(departmentSource).not.toContain('department-page__title');
    expect(departmentSource).not.toContain('department-page__hero');
    expect(departmentSource).not.toContain('department-page__desc');
    expect(departmentSource).toContain(':collapsible="false"');

    expect(positionSource).not.toContain('position-page__title');
    expect(positionSource).not.toContain('position-page__hero');
    expect(positionSource).not.toContain('position-page__desc');
    expect(positionSource).toContain(':collapsible="false"');
  });

  it('keeps department management as a collapsible tree table', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/department/department-list.vue',
      ),
      'utf8',
    );

    expect(source).toContain('children?: DepartmentTreeRow[];');
    expect(source).toContain('row-key="departmentId"');
    expect(source).toContain(':default-expand-all="true"');
    expect(source).toContain(':tree-props="{ children: \'children\' }"');
  });

  it('separates root department creation from child department creation', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/department/department-list.vue',
      ),
      'utf8',
    );

    expect(source).toContain('openAddRootDialog');
    expect(source).toContain('openAddChildDialog');
    expect(source).toContain('新增顶级部门');
    expect(source).toContain('新增下级');
    expect(source).toContain('parentId: row.departmentId');
    expect(source).toContain('dialogTitle');
  });

  it('locks department parent context and labels managers with department names', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/department/department-list.vue',
      ),
      'utf8',
    );

    expect(source).toContain('const isParentLocked = computed');
    expect(source).toContain(':disabled="isParentLocked"');
    expect(source).toContain('formatEmployeeOptionLabel');
    expect(source).toContain("item.departmentName || '未分配部门'");
    expect(source).toContain('return `${item.actualName}（${departmentName}）`;');
    expect(source).not.toContain(
      'return `${item.actualName} / ${departmentName} / ${item.loginName}`;',
    );
  });

  it.each(modulePages)(
    'uses the employee-management table header contract on $label',
    ({ path }) => {
      const pagePath = resolve(process.cwd(), path);
      const source = readFileSync(pagePath, 'utf8');

      expect(source).toContain('columnsFactory');
      expect(source).toContain('columnChecks');
      expect(source).toContain('v-model="columnChecks"');
      expect(source).not.toContain(':model-value="[]"');
      expect(source).toContain(
        'layout="search,size,fullscreen,columns,settings"',
      );
      expect(source).toContain('showSearchBar');
      expect(source).toContain('@search="handleToggleSearchBar"');
    },
  );

  it.each(actionPages)(
    'keeps row actions compact and measurable on $label',
    ({ actionClass, path }) => {
      const pagePath = resolve(process.cwd(), path);
      const source = readFileSync(pagePath, 'utf8');

      expect(source).toContain(`class="${actionClass}"`);
      expect(source).toContain(`.${actionClass} {`);
      expect(source).toContain('display: inline-flex;');
      expect(source).toContain('gap: 8px;');
      expect(source).toContain(`.${actionClass} :deep(.el-button)`);
      expect(source).toContain('font-size: 14px;');
      expect(source).toContain('line-height: 22px;');
      expect(source).toContain('padding: 0;');
      expect(source).toContain(`.${actionClass} :deep(.el-button + .el-button)`);
      expect(source).toContain('margin-left: 0;');
    },
  );
});
