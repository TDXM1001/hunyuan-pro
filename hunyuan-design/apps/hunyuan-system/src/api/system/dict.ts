import { requestClient } from '#/api/request';

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

export async function queryDictPage(params: DictPageQueryParams) {
  return requestClient.post<PageResult<DictRecord>>(
    '/support/dict/queryPage',
    buildDictPageQueryPayload(params),
  );
}

export async function addDict(params: DictAddForm) {
  return requestClient.post<string>(
    '/support/dict/add',
    buildDictMutationPayload(params),
  );
}

export async function updateDict(params: DictUpdateForm) {
  return requestClient.post<string>(
    '/support/dict/update',
    buildDictMutationPayload(params),
  );
}

export async function toggleDictDisabled(dictId: number) {
  return requestClient.get<string>(`/support/dict/updateDisabled/${dictId}`);
}

export async function batchDeleteDicts(dictIds: number[]) {
  return requestClient.post<string>('/support/dict/batchDelete', dictIds);
}

export async function deleteDict(dictId: number) {
  return requestClient.get<string>(`/support/dict/delete/${dictId}`);
}

export async function queryDictDataList(dictId: number) {
  return requestClient.get<DictDataRecord[]>(
    `/support/dict/dictData/queryDictData/${dictId}`,
  );
}

let allDictDataPromise: null | Promise<DictDataRecord[]> = null;

export async function queryAllDictData() {
  if (!allDictDataPromise) {
    allDictDataPromise = requestClient
      .get<DictDataRecord[]>('/support/dict/getAllDictData')
      .then((data) => data ?? [])
      .catch((error) => {
        allDictDataPromise = null;
        throw error;
      });
  }

  return allDictDataPromise;
}

export async function queryDictOptionsByCode(dictCode: string) {
  const allDictData = await queryAllDictData();
  return buildDictOptionsByCode(allDictData, dictCode);
}

export async function toggleDictDataDisabled(dictDataId: number) {
  return requestClient.get<string>(
    `/support/dict/dictData/updateDisabled/${dictDataId}`,
  );
}

export async function addDictData(params: DictDataAddForm) {
  return requestClient.post<string>(
    '/support/dict/dictData/add',
    buildDictDataMutationPayload(params),
  );
}

export async function updateDictData(params: DictDataUpdateForm) {
  return requestClient.post<string>(
    '/support/dict/dictData/update',
    buildDictDataMutationPayload(params),
  );
}

export async function batchDeleteDictData(dictDataIds: number[]) {
  return requestClient.post<string>(
    '/support/dict/dictData/batchDelete',
    dictDataIds,
  );
}

export async function deleteDictData(dictDataId: number) {
  return requestClient.get<string>(
    `/support/dict/dictData/delete/${dictDataId}`,
  );
}
