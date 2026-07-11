import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export interface BpmModelRecord {
  categoryId: number;
  categoryName?: null | string;
  createTime?: null | string;
  description?: null | string;
  formId: number;
  formName?: null | string;
  formType: number;
  hasUnpublishedChanges?: boolean;
  modelId: number;
  modelKey: string;
  modelName: string;
  publishedDefinitionId?: null | number;
  sort?: null | number;
  updateTime?: null | string;
  visibleFlag?: boolean;
}

export interface BpmModelPageQueryParams {
  categoryId?: null | number;
  formId?: null | number;
  modelKey?: string;
  modelName?: string;
  pageNum: number;
  pageSize: number;
  visibleFlag?: boolean;
}

export interface BpmModelAddForm {
  categoryId: number;
  description?: null | string;
  formId: number;
  formType: number;
  instanceNoRuleId?: null | number;
  modelKey: string;
  modelName: string;
  sort?: null | number;
  visibleFlag?: boolean;
}

export interface BpmModelUpdateForm extends BpmModelAddForm {
  modelId: number;
}

export interface BpmDesignerDetailRecord {
  categoryId: number;
  categoryName?: null | string;
  formId: number;
  formLayoutJson?: null | string;
  formName?: null | string;
  formSchemaJson?: null | string;
  formType: number;
  hasUnpublishedChanges?: boolean;
  instanceNoRuleId?: null | number;
  managerScopeJson?: null | string;
  modelId: number;
  modelKey: string;
  modelName: string;
  publishedDefinitionId?: null | number;
  simpleModelJson?: null | string;
  startRuleJson?: null | string;
  summaryRuleJson?: null | string;
  titleRuleJson?: null | string;
  variableMappingJson?: null | string;
}

export interface BpmDesignerSaveForm {
  managerScopeJson?: null | string;
  modelId: number;
  simpleModelJson: string;
  startRuleJson: string;
  summaryRuleJson?: null | string;
  titleRuleJson?: null | string;
  variableMappingJson?: null | string;
}

export interface BpmDefinitionPublishForm {
  modelId: number;
}

export interface BpmRouteExpressionDescriptor {
  expressionKey: string;
  name: string;
  parameterSchemaJson: string;
  version: number;
}

export function buildEmptyBpmDesignerDraft(): BpmDesignerSaveForm {
  return {
    managerScopeJson: '',
    modelId: 0,
    simpleModelJson: '{"nodes":[]}',
    startRuleJson: '{"type":"ALL"}',
    summaryRuleJson: '',
    titleRuleJson: '',
    variableMappingJson: '',
  };
}

export function buildBpmModelMutationPayload<
  T extends BpmModelAddForm | BpmModelUpdateForm,
>(params: T): T {
  return {
    ...params,
    description: params.description?.trim() || '',
    instanceNoRuleId: params.instanceNoRuleId ?? null,
    modelKey: params.modelKey.trim(),
    modelName: params.modelName.trim(),
    sort: params.sort ?? 0,
    visibleFlag: params.visibleFlag ?? true,
  };
}

export function buildBpmDesignerSavePayload(
  params: BpmDesignerSaveForm,
): BpmDesignerSaveForm {
  return {
    ...params,
    managerScopeJson: params.managerScopeJson?.trim() || '',
    simpleModelJson: params.simpleModelJson.trim(),
    startRuleJson: params.startRuleJson.trim(),
    summaryRuleJson: params.summaryRuleJson?.trim() || '',
    titleRuleJson: params.titleRuleJson?.trim() || '',
    variableMappingJson: params.variableMappingJson?.trim() || '',
  };
}

export async function queryBpmModelPage(params: BpmModelPageQueryParams) {
  return requestClient.post<PageResult<BpmModelRecord>>('/bpm/model/query', {
    categoryId: params.categoryId ?? undefined,
    formId: params.formId ?? undefined,
    modelKey: params.modelKey?.trim() || undefined,
    modelName: params.modelName?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    visibleFlag: params.visibleFlag,
  });
}

export async function getBpmModelDetail(modelId: number) {
  return requestClient.get<BpmModelRecord>(`/bpm/model/detail/${modelId}`);
}

export async function queryBpmRouteExpressionCatalog() {
  return requestClient.get<BpmRouteExpressionDescriptor[]>(
    '/bpm/model/route-expression/catalog',
  );
}

export async function addBpmModel(params: BpmModelAddForm) {
  return requestClient.post<string>(
    '/bpm/model/add',
    buildBpmModelMutationPayload(params),
  );
}

export async function updateBpmModel(params: BpmModelUpdateForm) {
  return requestClient.post<string>(
    '/bpm/model/update',
    buildBpmModelMutationPayload(params),
  );
}

export async function getBpmDesignerDetail(modelId: number) {
  return requestClient.get<BpmDesignerDetailRecord>(
    `/bpm/designer/detail/${modelId}`,
  );
}

export async function saveBpmDesignerDraft(params: BpmDesignerSaveForm) {
  return requestClient.post<string>(
    '/bpm/designer/save',
    buildBpmDesignerSavePayload(params),
  );
}

export async function validateBpmDesignerDraft(modelId: number) {
  return requestClient.get<string>(`/bpm/designer/validate/${modelId}`);
}

export async function simulateBpmDesignerDraft(modelId: number) {
  return requestClient.get<string>(`/bpm/designer/simulate/${modelId}`);
}

export async function publishBpmDefinition(params: BpmDefinitionPublishForm) {
  return requestClient.post<number>('/bpm/definition/publish', params);
}
