import type { RouteRecordStringComponent } from '@vben/types';

import { getLoginInfoApi } from './auth';

/**
 * 获取用户所有菜单
 */
export async function getAllMenusApi() {
  const { menuRoutes } = await getLoginInfoApi();
  return menuRoutes as RouteRecordStringComponent[];
}
