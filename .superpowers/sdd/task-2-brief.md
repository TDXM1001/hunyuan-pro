## Task 2: Add the Menu API Module

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes: `requestClient` from `#/api/request`.
- Produces:
  - `MenuRecord`
  - `MenuTreeRecord`
  - `MenuAddForm`
  - `MenuUpdateForm`
  - `RequestUrlRecord`
  - `queryMenus(): Promise<MenuRecord[]>`
  - `queryMenuTree(onlyMenu: boolean): Promise<MenuTreeRecord[]>`
  - `getMenuDetail(menuId: number): Promise<MenuRecord>`
  - `addMenu(params: MenuAddForm): Promise<string>`
  - `updateMenu(params: MenuUpdateForm): Promise<string>`
  - `batchDeleteMenus(menuIdList: number[]): Promise<string>`
  - `listAuthUrls(): Promise<RequestUrlRecord[]>`
  - `buildMenuMutationPayload<T extends MenuAddForm | MenuUpdateForm>(params: T): T`

- [ ] **Step 1: Create the API file**

Create `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts` with this content:

```ts
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
  method?: null | string;
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
  return requestClient.get<string>('/menu/batchDelete', {
    params: { menuIdList },
  });
}

export async function listAuthUrls() {
  return requestClient.get<RequestUrlRecord[]>('/menu/auth/url');
}
```

- [ ] **Step 2: Run the source contract**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
```

Expected: still FAIL because the Vue page does not exist, while the API endpoint assertions now pass.

- [ ] **Step 3: Run typecheck**

Run:

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts
git commit -m "feat: add system menu api module"
```

