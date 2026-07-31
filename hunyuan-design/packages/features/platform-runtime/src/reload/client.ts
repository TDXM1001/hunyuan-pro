import type { RequestClient } from '@vben/request';

const RELOAD_BASE_PATH = '/admin/v1/platform/runtime/reloads';

/** 刷新项配置和执行结果模型；tag 关联配置与历史执行记录。 */
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

// 刷新项由 identification + tag 标识，结果查询只使用 tag 作为稳定路径参数。
export function buildReloadMutationPayload(params: ReloadFormModel) {
  return {
    args: cleanText(params.args) || undefined,
    identification: cleanText(params.identification),
    tag: cleanText(params.tag),
  };
}

export function buildReloadResultPath(tag: string) {
  // tag 是刷新项的稳定业务标识，编码后拼到结果查询路径中。
  return `${RELOAD_BASE_PATH}/${encodeURIComponent(tag.trim())}/results`;
}

export async function queryReloadItems(requestClient: RequestClient) {
  // 获取所有可配置的刷新项，页面用它们展示 identification、tag 和参数。
  return requestClient.get<ReloadItemRecord[]>(RELOAD_BASE_PATH);
}

export async function updateReloadItem(requestClient: RequestClient, params: ReloadFormModel) {
  // 保存刷新项配置；参数为空时由 payload builder 省略，避免覆盖后端默认行为。
  return requestClient.put<string>(
    RELOAD_BASE_PATH,
    buildReloadMutationPayload(params),
  );
}

export async function queryReloadResults(requestClient: RequestClient, tag: string) {
  // 只查询当前 tag 的执行历史，避免结果抽屉展示其他刷新项的数据。
  return requestClient.get<ReloadResultRecord[]>(buildReloadResultPath(tag));
}
