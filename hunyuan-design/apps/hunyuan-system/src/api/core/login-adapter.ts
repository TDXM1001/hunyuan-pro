import type { RouteRecordStringComponent, UserInfo } from '@vben/types';

import { useAppConfig } from '@vben/hooks';

import { appFeatureRegistry } from '#/app-kernel/feature-registry';

const DEFAULT_HOME_PATH = '/system/home';
const MENU_TYPE_CATALOG = 1;
const MENU_TYPE_MENU = 2;
const NOT_FOUND_COMPONENT = '/_core/fallback/not-found';
const { apiURL: configuredApiUrl } = useAppConfig(
  import.meta.env,
  import.meta.env.PROD,
);
const API_BASE_URL = configuredApiUrl || '/api';

export interface BackendMenuItem {
  apiPerms?: null | string;
  cacheFlag?: boolean;
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
  routeId?: null | string;
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

function normalizeRoutePath(path?: null | string, fallbackId?: number) {
  if (!path) {
    return fallbackId ? `/system/menu-${fallbackId}` : DEFAULT_HOME_PATH;
  }
  return path.startsWith('/') ? path : `/${path}`;
}

function joinApiUrl(resourcePath: string) {
  const apiBase = API_BASE_URL.replace(/\/+$/, '');
  const normalizedPath = `/${resourcePath.replace(/^\/+/, '')}`;

  if (
    apiBase.startsWith('/') &&
    (normalizedPath === apiBase || normalizedPath.startsWith(`${apiBase}/`))
  ) {
    return normalizedPath;
  }
  return `${apiBase}${normalizedPath}`;
}

function normalizeAvatarUrl(url?: null | string) {
  if (!url) {
    return '';
  }

  try {
    const parsed = new URL(url);
    // 本地文件服务会返回其进程可见的绝对地址；浏览器必须经部署时 API 入口访问，不能绑定开发机地址。
    if (
      parsed.protocol === 'http:' &&
      (parsed.pathname === '/upload' || parsed.pathname.startsWith('/upload/'))
    ) {
      return joinApiUrl(`${parsed.pathname}${parsed.search}${parsed.hash}`);
    }
    return url;
  } catch {
    return url.startsWith('/') ? joinApiUrl(url) : url;
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

  if (node.routeId) {
    const registeredComponent = appFeatureRegistry.resolveComponent(
      node.routeId,
    );
    if (registeredComponent) {
      return {
        component: registeredComponent,
        meta: createBaseMeta(node, routePath),
        name: `BackendMenu${node.menuId}`,
        path: routePath,
      };
    }
    return {
      component: NOT_FOUND_COMPONENT,
      meta: createBaseMeta(node, routePath),
      name: `BackendMenu${node.menuId}`,
      path: routePath,
    };
  }

  return {
    component: NOT_FOUND_COMPONENT,
    meta: createBaseMeta(node, routePath),
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
