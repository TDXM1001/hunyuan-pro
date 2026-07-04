import { requestClient } from '#/api/request';

import {
  extractAccessCodes,
  mapLoginMenusToRoutes,
  mapLoginResultToUserInfo,
} from './login-adapter';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    loginDevice: number;
    loginName: string;
    password: string;
  }

  export interface LoginSession {
    accessCodes: string[];
    accessToken: string;
    menuRoutes: ReturnType<typeof mapLoginMenusToRoutes>;
    userInfo: ReturnType<typeof mapLoginResultToUserInfo>;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const loginResult = await requestClient.post<any>('/login', data);

  return {
    accessCodes: extractAccessCodes(loginResult.menuList ?? []),
    accessToken: loginResult.token ?? '',
    menuRoutes: mapLoginMenusToRoutes(loginResult.menuList ?? []),
    userInfo: mapLoginResultToUserInfo(loginResult),
  } satisfies AuthApi.LoginSession;
}

/**
 * 获取当前登录信息
 */
export async function getLoginInfoApi() {
  const loginResult = await requestClient.get<any>('/login/getLoginInfo');

  return {
    accessCodes: extractAccessCodes(loginResult.menuList ?? []),
    accessToken: loginResult.token ?? '',
    menuRoutes: mapLoginMenusToRoutes(loginResult.menuList ?? []),
    userInfo: mapLoginResultToUserInfo(loginResult),
  } satisfies AuthApi.LoginSession;
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return requestClient.get('/login/logout');
}
