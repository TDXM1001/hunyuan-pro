import type { PageResult } from '#/api/system/organization';
import type { BpmFormDesignerSnapshot } from '#/components/bpm/adapters/types';

import { requestClient } from '#/api/request';

export interface BpmFormRecord {
  createTime?: null | string;
  disabledFlag?: boolean;
  formId: number;
  formKey: string;
  formName: string;
  layoutJson?: null | string;
  remark?: null | string;
  schemaJson: string;
  updateTime?: null | string;
}

export interface BpmFormPageQueryParams {
  disabledFlag?: boolean;
  formKey?: string;
  formName?: string;
  pageNum: number;
  pageSize: number;
}

export interface BpmFormAddForm {
  disabledFlag?: boolean;
  formKey: string;
  formName: string;
  layoutJson?: null | string;
  remark?: null | string;
  schemaJson: string;
}

export interface BpmFormUpdateForm extends BpmFormAddForm {
  formId: number;
}

export function buildEmptyBpmFormDesignerSnapshot(): BpmFormDesignerSnapshot {
  return {
    layoutJson: '{}',
    schemaJson: '[]',
  };
}

export function buildBpmFormMutationPayload<
  T extends BpmFormAddForm | BpmFormUpdateForm,
>(params: T): T {
  return {
    ...params,
    disabledFlag: params.disabledFlag ?? false,
    formKey: params.formKey.trim(),
    formName: params.formName.trim(),
    layoutJson: params.layoutJson?.trim() || '',
    remark: params.remark?.trim() || '',
    schemaJson: params.schemaJson.trim(),
  };
}

export async function queryBpmFormPage(params: BpmFormPageQueryParams) {
  return requestClient.post<PageResult<BpmFormRecord>>('/bpm/form/query', {
    disabledFlag: params.disabledFlag,
    formKey: params.formKey?.trim() || undefined,
    formName: params.formName?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  });
}

export async function getBpmFormDetail(formId: number) {
  return requestClient.get<BpmFormRecord>(`/bpm/form/detail/${formId}`);
}

export async function addBpmForm(params: BpmFormAddForm) {
  return requestClient.post<string>(
    '/bpm/form/add',
    buildBpmFormMutationPayload(params),
  );
}

export async function updateBpmForm(params: BpmFormUpdateForm) {
  return requestClient.post<string>(
    '/bpm/form/update',
    buildBpmFormMutationPayload(params),
  );
}
