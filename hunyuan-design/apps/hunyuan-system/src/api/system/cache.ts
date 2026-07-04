import { requestClient } from '#/api/request';

export function buildCacheKeysPath(cacheName: string) {
  return `/support/cache/keys/${encodeURIComponent(cacheName.trim())}`;
}

export function buildCacheRemovePath(cacheName: string) {
  return `/support/cache/remove/${encodeURIComponent(cacheName.trim())}`;
}

export async function queryCacheNames() {
  return requestClient.get<string[]>('/support/cache/names');
}

export async function queryCacheKeys(cacheName: string) {
  return requestClient.get<string[]>(buildCacheKeysPath(cacheName));
}

export async function removeCache(cacheName: string) {
  return requestClient.get<string>(buildCacheRemovePath(cacheName));
}
