import { requestClient } from '#/api/request';

const RELOAD_BASE_PATH = '/admin/v1/platform/runtime/reloads';

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
  return `${RELOAD_BASE_PATH}/${encodeURIComponent(tag.trim())}/results`;
}

export async function queryReloadItems() {
  return requestClient.get<ReloadItemRecord[]>(RELOAD_BASE_PATH);
}

export async function updateReloadItem(params: ReloadFormModel) {
  return requestClient.put<string>(
    RELOAD_BASE_PATH,
    buildReloadMutationPayload(params),
  );
}

export async function queryReloadResults(tag: string) {
  return requestClient.get<ReloadResultRecord[]>(buildReloadResultPath(tag));
}
