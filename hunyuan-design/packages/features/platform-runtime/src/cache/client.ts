import type { RequestClient } from '@vben/request';

// 缓存名称进入路径前统一编码，避免名称中的空格或斜杠改变管理接口的路由结构。
export function buildCacheKeysPath(cacheName: string) {
  // 根据缓存名称生成“查看 keys”的路径；名称先 trim 和 URL 编码，避免破坏路由层级。
  return `/support/cache/keys/${encodeURIComponent(cacheName.trim())}`;
}

export function buildCacheRemovePath(cacheName: string) {
  // 根据缓存名称生成“删除缓存”的路径，删除动作由页面确认后调用。
  return `/support/cache/remove/${encodeURIComponent(cacheName.trim())}`;
}

export async function queryCacheNames(requestClient: RequestClient) {
  // 获取当前系统可管理的缓存名称列表。
  return requestClient.get<string[]>('/support/cache/names');
}

export async function queryCacheKeys(requestClient: RequestClient, cacheName: string) {
  // 获取某一缓存名称下的 key 列表，供抽屉内检索和查看。
  return requestClient.get<string[]>(buildCacheKeysPath(cacheName));
}

export async function removeCache(requestClient: RequestClient, cacheName: string) {
  // 删除指定缓存名称下的缓存内容，返回后端的处理结果文本。
  return requestClient.get<string>(buildCacheRemovePath(cacheName));
}
