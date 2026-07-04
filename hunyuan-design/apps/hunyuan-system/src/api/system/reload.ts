import { requestClient } from '#/api/request';

export interface ReloadItemRecord {
  args?: null | string;
  createTime?: null | string;
  identification: string;
  tag: string;
  updateTime?: null | string;
}

export interface ReloadResultRecord {
  args?: null | string;
  createTime?: null | string;
  exception?: null | string;
  result?: null | boolean;
  tag: string;
}

export interface ReloadFormModel {
  args?: null | string;
  identification: string;
  tag: string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildReloadMutationPayload(params: ReloadFormModel) {
  return {
    args: cleanText(params.args) || undefined,
    identification: cleanText(params.identification),
    tag: cleanText(params.tag),
  };
}

export function buildReloadResultPath(tag: string) {
  return `/support/reload/result/${encodeURIComponent(tag.trim())}`;
}

export async function queryReloadItems() {
  return requestClient.get<ReloadItemRecord[]>('/support/reload/query');
}

export async function updateReloadItem(params: ReloadFormModel) {
  return requestClient.post<string>(
    '/support/reload/update',
    buildReloadMutationPayload(params),
  );
}

export async function queryReloadResults(tag: string) {
  return requestClient.get<ReloadResultRecord[]>(buildReloadResultPath(tag));
}
