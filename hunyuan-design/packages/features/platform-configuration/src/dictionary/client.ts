import type { RequestClient } from '@vben/request';

/**
 * 字典管理的数据模型。
 * DictRecord 是字典主表，DictDataRecord 是字典项；业务页面通常只消费 DictOption 这种简单选项。
 */
export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface DictRecord {
  dictId: number;
  dictName: string;
  dictCode: string;
  remark?: null | string;
  disabledFlag?: boolean;
  createTime?: null | string;
  updateTime?: null | string;
}

export interface DictDataRecord {
  dictDataId: number;
  dictId: number;
  dictCode: string;
  dictName?: null | string;
  dictDisabledFlag?: boolean;
  dataValue: string;
  dataLabel: string;
  dataStyle?: null | string;
  remark?: null | string;
  sortOrder: number;
  disabledFlag?: boolean;
  createTime?: null | string;
  updateTime?: null | string;
}

export interface DictOption {
  label: string;
  value: string;
}

export interface DictPageQueryParams {
  keywords?: string;
  disabledFlag?: boolean;
  pageNum: number;
  pageSize: number;
}

export interface DictAddForm {
  dictName: string;
  dictCode: string;
  remark?: null | string;
}

export interface DictUpdateForm extends DictAddForm {
  dictId: number;
}

export interface DictDataAddForm {
  dictId: number;
  dataValue: string;
  dataLabel: string;
  dataStyle?: null | string;
  remark?: null | string;
  sortOrder: number;
}

export interface DictDataUpdateForm extends DictDataAddForm {
  dictDataId: number;
  dictCode: string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildDictPageQueryPayload(params: DictPageQueryParams) {
  /** 查询字典主表，keywords 为空时让后端忽略关键词筛选。 */
  const keywords = cleanText(params.keywords);

  return {
    disabledFlag: params.disabledFlag,
    keywords: keywords || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export function buildDictMutationPayload<T extends DictAddForm | DictUpdateForm>(
  params: T,
): T {
  return {
    ...params,
    dictCode: params.dictCode.trim(),
    dictName: params.dictName.trim(),
    remark: cleanText(params.remark),
  };
}

export function buildDictDataMutationPayload<
  T extends DictDataAddForm | DictDataUpdateForm,
>(params: T): T {
  return {
    ...params,
    dataLabel: params.dataLabel.trim(),
    dataStyle: cleanText(params.dataStyle) || undefined,
    dataValue: params.dataValue.trim(),
    remark: cleanText(params.remark),
  };
}

export function buildDictOptionsByCode(
  records: DictDataRecord[],
  dictCode: string,
): DictOption[] {
  // 字典选项只暴露启用项，并按业务排序号稳定排序；页面不应重复实现这套过滤规则。
  const normalizedDictCode = cleanText(dictCode);
  if (!normalizedDictCode) {
    return [];
  }

  return records
    .filter((item) => (
      item.dictCode === normalizedDictCode
      && !item.dictDisabledFlag
      && !item.disabledFlag
    ))
    .sort((left, right) => (
      left.sortOrder - right.sortOrder
      || left.dataLabel.localeCompare(right.dataLabel, 'zh-CN')
    ))
    .map((item) => ({
      label: item.dataLabel,
      value: item.dataValue,
    }));
}

export async function queryDictPage(
  requestClient: RequestClient,
  params: DictPageQueryParams,
) {
  // 主表接口只返回字典本身；字典项通过下方独立接口和抽屉加载。
  return requestClient.post<PageResult<DictRecord>>(
    '/admin/v1/platform/dictionaries/query',
    buildDictPageQueryPayload(params),
  );
}

export async function addDict(
  requestClient: RequestClient,
  params: DictAddForm,
) {
  // 新增字典由服务端生成 dictId，客户端只提交名称、编码和备注。
  return requestClient.post<string>(
    '/admin/v1/platform/dictionaries',
    buildDictMutationPayload(params),
  );
}

export async function updateDict(
  requestClient: RequestClient,
  params: DictUpdateForm,
) {
  // 更新路径使用 dictId，避免用户修改名称或编码时误定位其他字典。
  return requestClient.put<string>(
    `/admin/v1/platform/dictionaries/${params.dictId}`,
    buildDictMutationPayload(params),
  );
}

export async function toggleDictDisabled(
  requestClient: RequestClient,
  dictId: number,
) {
  // 禁用是状态切换动作，不把完整字典对象重新提交，减少覆盖其他字段的风险。
  return requestClient.post<string>(
    `/admin/v1/platform/dictionaries/${dictId}/toggle-disabled`,
  );
}

export async function batchDeleteDicts(
  requestClient: RequestClient,
  dictIds: number[],
) {
  // 批量删除只提交主键数组，具体关联字典项和引用校验由后端负责。
  return requestClient.post<string>(
    '/admin/v1/platform/dictionaries/batch-delete',
    dictIds,
  );
}

export async function deleteDict(requestClient: RequestClient, dictId: number) {
  // 删除单个字典时只使用主键，不复用列表中的展示字段。
  return requestClient.delete<string>(`/admin/v1/platform/dictionaries/${dictId}`);
}

export async function queryDictDataList(
  requestClient: RequestClient,
  dictId: number,
) {
  // 打开某个字典的项管理抽屉时，按 dictId 查询它当前的全部选项。
  return requestClient.get<DictDataRecord[]>(
    `/admin/v1/platform/dictionaries/${dictId}/items`,
  );
}

const allDictDataPromises = new WeakMap<
  RequestClient,
  Promise<DictDataRecord[]>
>();

export async function queryAllDictData(requestClient: RequestClient) {
  // 为下拉选项提供一次性全量字典项查询；同一个注入客户端复用 Promise，避免重复请求。
  let allDictDataPromise = allDictDataPromises.get(requestClient);
  if (!allDictDataPromise) {
    // 以注入的客户端隔离缓存，防止未来多应用装配时复用错误的会话数据。
    // 请求失败时删除缓存，下一次调用才能重试，而不是永久复用失败的 Promise。
    allDictDataPromise = requestClient
      .get<DictDataRecord[]>('/admin/v1/platform/dictionaries/items')
      .then((data) => data ?? [])
      .catch((error) => {
        allDictDataPromises.delete(requestClient);
        throw error;
      });
    allDictDataPromises.set(requestClient, allDictDataPromise);
  }

  return allDictDataPromise;
}

export async function queryDictOptionsByCode(
  requestClient: RequestClient,
  dictCode: string,
) {
  // 组合“全量读取 + 按编码过滤”，供业务页面直接得到可用于 Select 的 label/value 数组。
  const allDictData = await queryAllDictData(requestClient);
  return buildDictOptionsByCode(allDictData, dictCode);
}

export async function toggleDictDataDisabled(
  requestClient: RequestClient,
  dictDataId: number,
) {
  // 字典项禁用同样是独立状态操作，不重新提交字典项正文。
  return requestClient.post<string>(
    `/admin/v1/platform/dictionaries/items/${dictDataId}/toggle-disabled`,
  );
}

export async function addDictData(
  requestClient: RequestClient,
  params: DictDataAddForm,
) {
  // 字典项归属于 dictId 对应的主字典，新增路径因此包含父级 ID。
  return requestClient.post<string>(
    `/admin/v1/platform/dictionaries/${params.dictId}/items`,
    buildDictDataMutationPayload(params),
  );
}

export async function updateDictData(
  requestClient: RequestClient,
  params: DictDataUpdateForm,
) {
  // 更新同时使用 dictId 和 dictDataId，防止跨字典修改到同编号的历史数据。
  return requestClient.put<string>(
    `/admin/v1/platform/dictionaries/${params.dictId}/items/${params.dictDataId}`,
    buildDictDataMutationPayload(params),
  );
}

export async function batchDeleteDictData(
  requestClient: RequestClient,
  dictDataIds: number[],
) {
  // 批量删除字典项只传选中的项主键，页面不负责判断是否仍被业务使用。
  return requestClient.post<string>(
    '/admin/v1/platform/dictionaries/items/batch-delete',
    dictDataIds,
  );
}

export async function deleteDictData(
  requestClient: RequestClient,
  dictDataId: number,
) {
  // 删除单个字典项的路径使用 dictDataId，保持与批量删除相同的数据边界。
  return requestClient.delete<string>(
    `/admin/v1/platform/dictionaries/items/${dictDataId}`,
  );
}
