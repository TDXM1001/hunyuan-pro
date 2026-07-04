import type { UserInfo } from '@vben/types';

import { getLoginInfoApi } from './auth';

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  const { userInfo } = await getLoginInfoApi();
  return userInfo as UserInfo;
}
