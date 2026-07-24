import type { RequestClient } from '@vben/request';

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
  return requestClient.post<PageResult<DictRecord>>(
    '/admin/v1/platform/dictionaries/query',
    buildDictPageQueryPayload(params),
  );
}

export async function addDict(
  requestClient: RequestClient,
  params: DictAddForm,
) {
  return requestClient.post<string>(
    '/admin/v1/platform/dictionaries',
    buildDictMutationPayload(params),
  );
}

export async function updateDict(
  requestClient: RequestClient,
  params: DictUpdateForm,
) {
  return requestClient.put<string>(
    `/admin/v1/platform/dictionaries/${params.dictId}`,
    buildDictMutationPayload(params),
  );
}

export async function toggleDictDisabled(
  requestClient: RequestClient,
  dictId: number,
) {
  return requestClient.post<string>(
    `/admin/v1/platform/dictionaries/${dictId}/toggle-disabled`,
  );
}

export async function batchDeleteDicts(
  requestClient: RequestClient,
  dictIds: number[],
) {
  return requestClient.post<string>(
    '/admin/v1/platform/dictionaries/batch-delete',
    dictIds,
  );
}

export async function deleteDict(requestClient: RequestClient, dictId: number) {
  return requestClient.delete<string>(`/admin/v1/platform/dictionaries/${dictId}`);
}

export async function queryDictDataList(
  requestClient: RequestClient,
  dictId: number,
) {
  return requestClient.get<DictDataRecord[]>(
    `/admin/v1/platform/dictionaries/${dictId}/items`,
  );
}

const allDictDataPromises = new WeakMap<
  RequestClient,
  Promise<DictDataRecord[]>
>();

export async function queryAllDictData(requestClient: RequestClient) {
  let allDictDataPromise = allDictDataPromises.get(requestClient);
  if (!allDictDataPromise) {
    // 以注入的客户端隔离缓存，防止未来多应用装配时复用错误的会话数据。
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
  const allDictData = await queryAllDictData(requestClient);
  return buildDictOptionsByCode(allDictData, dictCode);
}

export async function toggleDictDataDisabled(
  requestClient: RequestClient,
  dictDataId: number,
) {
  return requestClient.post<string>(
    `/admin/v1/platform/dictionaries/items/${dictDataId}/toggle-disabled`,
  );
}

export async function addDictData(
  requestClient: RequestClient,
  params: DictDataAddForm,
) {
  return requestClient.post<string>(
    `/admin/v1/platform/dictionaries/${params.dictId}/items`,
    buildDictDataMutationPayload(params),
  );
}

export async function updateDictData(
  requestClient: RequestClient,
  params: DictDataUpdateForm,
) {
  return requestClient.put<string>(
    `/admin/v1/platform/dictionaries/${params.dictId}/items/${params.dictDataId}`,
    buildDictDataMutationPayload(params),
  );
}

export async function batchDeleteDictData(
  requestClient: RequestClient,
  dictDataIds: number[],
) {
  return requestClient.post<string>(
    '/admin/v1/platform/dictionaries/items/batch-delete',
    dictDataIds,
  );
}

export async function deleteDictData(
  requestClient: RequestClient,
  dictDataId: number,
) {
  return requestClient.delete<string>(
    `/admin/v1/platform/dictionaries/items/${dictDataId}`,
  );
}
