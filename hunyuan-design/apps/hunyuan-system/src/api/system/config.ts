import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface ConfigRecord {
  configId: number;
  configKey: string;
  configName: string;
  configValue: string;
  remark?: null | string;
  createTime?: null | string;
  updateTime?: null | string;
}

export interface ConfigPageQueryParams {
  configKey?: string;
  pageNum: number;
  pageSize: number;
}

export interface ConfigAddForm {
  configKey: string;
  configName: string;
  configValue: string;
  remark?: null | string;
}

export interface ConfigUpdateForm extends ConfigAddForm {
  configId: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildConfigPageQueryPayload(params: ConfigPageQueryParams) {
  const configKey = cleanText(params.configKey);

  return {
    configKey: configKey || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export function buildConfigMutationPayload<
  T extends ConfigAddForm | ConfigUpdateForm,
>(params: T): T {
  return {
    ...params,
    configKey: params.configKey.trim(),
    configName: params.configName.trim(),
    configValue: params.configValue.trim(),
    remark: cleanText(params.remark),
  };
}

export async function queryConfigPage(params: ConfigPageQueryParams) {
  return requestClient.post<PageResult<ConfigRecord>>(
    '/support/config/query',
    buildConfigPageQueryPayload(params),
  );
}

export async function addConfig(params: ConfigAddForm) {
  return requestClient.post<string>(
    '/support/config/add',
    buildConfigMutationPayload(params),
  );
}

export async function updateConfig(params: ConfigUpdateForm) {
  return requestClient.post<string>(
    '/support/config/update',
    buildConfigMutationPayload(params),
  );
}
