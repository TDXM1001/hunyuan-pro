import type { RouteRecordStringComponent, UserInfo } from '@vben/types';

const DEFAULT_HOME_PATH = '/system/home';
const MENU_TYPE_CATALOG = 1;
const MENU_TYPE_MENU = 2;
const LOCAL_API_ORIGIN = 'http://127.0.0.1:1024';
const MODULE_BRIDGE_COMPONENT = '/system/module-bridge/index';

const localViewModules = import.meta.glob('../../views/**/*.vue');
const localViewPathSet = new Set(
  Object.keys(localViewModules).map((path) => normalizeViewModulePath(path)),
);

export interface BackendMenuItem {
  apiPerms?: null | string;
  cacheFlag?: boolean;
  component?: null | string;
  contextMenuId?: null | number;
  disabledFlag?: boolean;
  frameFlag?: boolean;
  frameUrl?: null | string;
  icon?: null | string;
  menuId: number;
  menuName: string;
  menuType?: null | number;
  parentId?: null | number;
  path?: null | string;
  permsType?: null | number;
  sort?: null | number;
  visibleFlag?: boolean;
  webPerms?: null | string;
}

export interface BackendLoginResult {
  actualName?: null | string;
  administratorFlag?: boolean;
  avatar?: null | string;
  departmentName?: null | string;
  employeeId?: null | number;
  loginName?: null | string;
  menuList?: BackendMenuItem[];
  needUpdatePwdFlag?: boolean;
  token?: null | string;
}

interface MenuNode extends BackendMenuItem {
  children: MenuNode[];
}

function normalizeViewModulePath(path: string) {
  return path
    .replace(/\\/g, '/')
    .replace(/^.*?views\//, '/')
    .replace(/\.vue$/i, '');
}

function normalizeComponentPath(component?: null | string) {
  if (!component) {
    return '';
  }

  const normalized = component
    .replace(/^#\//, '')
    .replace(/^\/?views\//, '')
    .replace(/^\//, '')
    .replace(/\.vue$/i, '');

  return normalized ? `/${normalized}` : '';
}

function normalizeRoutePath(path?: null | string, fallbackId?: number) {
  if (!path) {
    return fallbackId ? `/system/menu-${fallbackId}` : DEFAULT_HOME_PATH;
  }
  return path.startsWith('/') ? path : `/${path}`;
}

function normalizeAvatarUrl(url?: null | string) {
  if (!url) {
    return '';
  }

  try {
    const parsed = new URL(url);
    if (parsed.hostname === '198.18.0.1') {
      return `${LOCAL_API_ORIGIN}${parsed.pathname}${parsed.search}`;
    }
    return url;
  } catch {
    return url.startsWith('/') ? `${LOCAL_API_ORIGIN}${url}` : url;
  }
}

function splitPerms(perms?: null | string) {
  if (!perms) {
    return [];
  }
  return perms
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function isExternalLink(value?: null | string) {
  return Boolean(value && /^https?:\/\//i.test(value));
}

function hasLocalView(componentPath: string) {
  return localViewPathSet.has(componentPath);
}

function buildMenuTree(menuList: BackendMenuItem[]) {
  const routeMenus = menuList
    .filter(
      (item) =>
        item.menuType === MENU_TYPE_CATALOG || item.menuType === MENU_TYPE_MENU,
    )
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  const nodeMap = new Map<number, MenuNode>();
  routeMenus.forEach((item) => {
    nodeMap.set(item.menuId, {
      ...item,
      children: [],
    });
  });

  const roots: MenuNode[] = [];
  nodeMap.forEach((node) => {
    const parentId = node.parentId ?? 0;
    const parent = parentId ? nodeMap.get(parentId) : undefined;
    if (parent) {
      parent.children.push(node);
      return;
    }
    roots.push(node);
  });

  return roots;
}

function createBaseMeta(node: MenuNode, routePath: string) {
  return {
    affixTab: routePath === DEFAULT_HOME_PATH,
    hideInMenu: node.visibleFlag === false,
    icon: node.icon || undefined,
    keepAlive: node.cacheFlag ?? false,
    title: node.menuName,
  };
}

function createBridgeMeta(node: MenuNode, routePath: string) {
  return {
    ...createBaseMeta(node, routePath),
    backendMenu: {
      backendComponent: node.component || '',
      backendFrameUrl: node.frameUrl || '',
      backendPath: routePath,
      menuId: node.menuId,
      menuType: node.menuType ?? null,
      parentId: node.parentId ?? null,
      title: node.menuName,
    },
  };
}

function mapNodeToRoute(node: MenuNode): null | RouteRecordStringComponent {
  const routePath = normalizeRoutePath(node.path, node.menuId);
  const childRoutes = node.children
    .map((child) => mapNodeToRoute(child))
    .filter((item): item is RouteRecordStringComponent => Boolean(item));

  if (childRoutes.length > 0) {
    return {
      children: childRoutes,
      component: 'BasicLayout',
      meta: createBaseMeta(node, routePath),
      name: `BackendMenu${node.menuId}`,
      path: routePath,
    };
  }

  if (node.frameFlag && node.frameUrl) {
    return {
      component: 'IFrameView',
      meta: {
        ...createBaseMeta(node, routePath),
        link: node.frameUrl,
      },
      name: `BackendMenu${node.menuId}`,
      path: routePath,
    };
  }

  if (isExternalLink(node.component)) {
    return {
      component: 'IFrameView',
      meta: {
        ...createBaseMeta(node, routePath),
        link: node.component || undefined,
      },
      name: `BackendMenu${node.menuId}`,
      path: routePath,
    };
  }

  const componentPath = normalizeComponentPath(node.component);
  if (componentPath && hasLocalView(componentPath)) {
    return {
      component: componentPath,
      meta: createBaseMeta(node, routePath),
      name: `BackendMenu${node.menuId}`,
      path: routePath,
    };
  }

  return {
    component: MODULE_BRIDGE_COMPONENT,
    meta: createBridgeMeta(node, routePath),
    name: `BackendMenu${node.menuId}`,
    path: routePath,
  };
}

function createDefaultHomeRoute(): RouteRecordStringComponent {
  return {
    children: [
      {
        component: '/system/home/index',
        meta: {
          affixTab: true,
          icon: 'HomeOutlined',
          title: '首页',
        },
        name: 'SystemHomeFallback',
        path: DEFAULT_HOME_PATH,
      },
    ],
    component: 'BasicLayout',
    meta: {
      icon: 'HomeOutlined',
      title: '工作台',
    },
    name: 'SystemFallbackRoot',
    path: '/system',
  };
}

function ensureDefaultHomeRoute(routes: RouteRecordStringComponent[]) {
  const hasHomeRoute = routes.some((route) => {
    if (route.path === DEFAULT_HOME_PATH) {
      return true;
    }
    return route.children?.some((child) => child.path === DEFAULT_HOME_PATH);
  });

  if (hasHomeRoute) {
    return routes;
  }

  return [createDefaultHomeRoute(), ...routes];
}

export function mapLoginMenusToRoutes(menuList: BackendMenuItem[] = []) {
  const menuTree = buildMenuTree(menuList);
  const routes = menuTree
    .map((node) => mapNodeToRoute(node))
    .filter((item): item is RouteRecordStringComponent => Boolean(item));

  return ensureDefaultHomeRoute(routes);
}

export function extractAccessCodes(menuList: BackendMenuItem[] = []) {
  return Array.from(
    new Set(menuList.flatMap((item) => splitPerms(item.webPerms))),
  );
}

export function mapLoginResultToUserInfo(
  loginResult: BackendLoginResult,
): UserInfo {
  return {
    avatar: normalizeAvatarUrl(loginResult.avatar),
    desc: loginResult.departmentName ?? '',
    homePath: DEFAULT_HOME_PATH,
    realName: loginResult.actualName ?? loginResult.loginName ?? 'Admin',
    roles: loginResult.administratorFlag ? ['admin'] : [],
    token: loginResult.token ?? '',
    userId: String(loginResult.employeeId ?? ''),
    username: loginResult.loginName ?? '',
  };
}
