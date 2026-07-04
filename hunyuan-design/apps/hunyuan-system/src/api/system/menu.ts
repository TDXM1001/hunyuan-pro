import { requestClient } from '#/api/request';

export interface MenuRecord {
  apiPerms?: null | string;
  cacheFlag: boolean;
  component?: null | string;
  contextMenuId?: null | number;
  createTime?: null | string;
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
  sort?: null | number;
  updateTime?: null | string;
  visibleFlag: boolean;
  webPerms?: null | string;
}

export interface MenuTreeRecord extends MenuRecord {
  children?: MenuTreeRecord[];
}

export interface RequestUrlRecord {
  comment?: null | string;
  name?: null | string;
  url?: null | string;
}

export interface MenuAddForm {
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
  sort?: null | number;
  visibleFlag: boolean;
  webPerms?: null | string;
}

export interface MenuUpdateForm extends MenuAddForm {
  menuId: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

function buildRepeatedQueryString(
  key: string,
  values: Array<number | string>,
) {
  const searchParams = new URLSearchParams();

  values.forEach((value) => {
    searchParams.append(key, String(value));
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export function buildMenuMutationPayload<
  T extends MenuAddForm | MenuUpdateForm,
>(params: T): T {
  return {
    ...params,
    apiPerms: cleanText(params.apiPerms),
    component: cleanText(params.component),
    contextMenuId: params.contextMenuId ?? null,
    frameUrl: cleanText(params.frameUrl),
    icon: cleanText(params.icon),
    menuName: params.menuName.trim(),
    parentId: params.parentId ?? 0,
    path: cleanText(params.path),
    permsType: params.permsType ?? null,
    sort: params.sort ?? 0,
    webPerms: cleanText(params.webPerms),
  };
}

export async function queryMenus() {
  return requestClient.get<MenuRecord[]>('/menu/query');
}

export async function queryMenuTree(onlyMenu: boolean) {
  return requestClient.get<MenuTreeRecord[]>('/menu/tree', {
    params: { onlyMenu },
  });
}

export async function getMenuDetail(menuId: number) {
  return requestClient.get<MenuRecord>(`/menu/detail/${menuId}`);
}

export async function addMenu(params: MenuAddForm) {
  return requestClient.post<string>(
    '/menu/add',
    buildMenuMutationPayload(params),
  );
}

export async function updateMenu(params: MenuUpdateForm) {
  return requestClient.post<string>(
    '/menu/update',
    buildMenuMutationPayload(params),
  );
}

export async function batchDeleteMenus(menuIdList: number[]) {
  return requestClient.get<string>(
    `/menu/batchDelete${buildRepeatedQueryString('menuIdList', menuIdList)}`,
  );
}

export async function listAuthUrls() {
  return requestClient.get<RequestUrlRecord[]>('/menu/auth/url');
}
