import type { RequestClient } from '@vben/request';

/** 平台参数配置的数据模型和请求转换规则。配置值以字符串保存，业务解释由配置 key 决定。 */
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

// 查询条件和备注字段的空白值统一归一为可省略值，避免后端收到无意义筛选条件。
function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildConfigPageQueryPayload(params: ConfigPageQueryParams) {
  // 配置查询只按 configKey 和分页过滤，空 key 会被转换为 undefined 以表示“不筛选”。
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
  // 配置值虽然是文本，但 key/name/value 都属于契约字段，统一裁剪后再提交，避免产生不可见差异。
  return {
    ...params,
    configKey: params.configKey.trim(),
    configName: params.configName.trim(),
    configValue: params.configValue.trim(),
    remark: cleanText(params.remark),
  };
}

export async function queryConfigPage(
  requestClient: RequestClient,
  params: ConfigPageQueryParams,
) {
  // 获取平台参数配置列表，页面根据返回的 list 和 total 渲染表格与分页器。
  return requestClient.post<PageResult<ConfigRecord>>(
    '/admin/v1/platform/configurations/query',
    buildConfigPageQueryPayload(params),
  );
}

export async function addConfig(
  requestClient: RequestClient,
  params: ConfigAddForm,
) {
  // 新增配置由后端生成 configId，前端只提交配置键、名称、值和备注。
  return requestClient.post<string>(
    '/admin/v1/platform/configurations',
    buildConfigMutationPayload(params),
  );
}

export async function updateConfig(
  requestClient: RequestClient,
  params: ConfigUpdateForm,
) {
  // 更新路径使用 configId，避免 configKey 被修改后无法准确定位旧记录。
  return requestClient.put<string>(
    `/admin/v1/platform/configurations/${params.configId}`,
    buildConfigMutationPayload(params),
  );
}
