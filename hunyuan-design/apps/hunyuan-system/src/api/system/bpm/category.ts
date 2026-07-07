import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export interface BpmCategoryRecord {
  categoryCode: string;
  categoryId: number;
  categoryName: string;
  createTime?: null | string;
  disabledFlag?: boolean;
  icon?: null | string;
  remark?: null | string;
  sort?: null | number;
  updateTime?: null | string;
}

export interface BpmCategoryPageQueryParams {
  categoryCode?: string;
  categoryName?: string;
  disabledFlag?: boolean;
  pageNum: number;
  pageSize: number;
}

export interface BpmCategoryAddForm {
  categoryCode: string;
  categoryName: string;
  disabledFlag?: boolean;
  icon?: null | string;
  remark?: null | string;
  sort?: null | number;
}

export interface BpmCategoryUpdateForm extends BpmCategoryAddForm {
  categoryId: number;
}

export function buildBpmCategoryMutationPayload<
  T extends BpmCategoryAddForm | BpmCategoryUpdateForm,
>(params: T): T {
  return {
    ...params,
    categoryCode: params.categoryCode.trim(),
    categoryName: params.categoryName.trim(),
    disabledFlag: params.disabledFlag ?? false,
    icon: params.icon?.trim() || '',
    remark: params.remark?.trim() || '',
    sort: params.sort ?? 0,
  };
}

export async function queryBpmCategoryPage(params: BpmCategoryPageQueryParams) {
  return requestClient.post<PageResult<BpmCategoryRecord>>('/bpm/category/query', {
    categoryCode: params.categoryCode?.trim() || undefined,
    categoryName: params.categoryName?.trim() || undefined,
    disabledFlag: params.disabledFlag,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  });
}

export async function getBpmCategoryDetail(categoryId: number) {
  return requestClient.get<BpmCategoryRecord>(`/bpm/category/detail/${categoryId}`);
}

export async function addBpmCategory(params: BpmCategoryAddForm) {
  return requestClient.post<string>(
    '/bpm/category/add',
    buildBpmCategoryMutationPayload(params),
  );
}

export async function updateBpmCategory(params: BpmCategoryUpdateForm) {
  return requestClient.post<string>(
    '/bpm/category/update',
    buildBpmCategoryMutationPayload(params),
  );
}
