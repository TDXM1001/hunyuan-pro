import type { RequestClient } from '@vben/request';

export function buildCacheKeysPath(cacheName: string) {
  return `/support/cache/keys/${encodeURIComponent(cacheName.trim())}`;
}

export function buildCacheRemovePath(cacheName: string) {
  return `/support/cache/remove/${encodeURIComponent(cacheName.trim())}`;
}

export async function queryCacheNames(requestClient: RequestClient) {
  return requestClient.get<string[]>('/support/cache/names');
}

export async function queryCacheKeys(requestClient: RequestClient, cacheName: string) {
  return requestClient.get<string[]>(buildCacheKeysPath(cacheName));
}

export async function removeCache(requestClient: RequestClient, cacheName: string) {
  return requestClient.get<string>(buildCacheRemovePath(cacheName));
}
