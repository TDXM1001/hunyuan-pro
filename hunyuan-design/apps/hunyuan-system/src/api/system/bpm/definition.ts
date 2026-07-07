import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export interface BpmDefinitionRecord {
  categoryNameSnapshot?: null | string;
  definitionId: number;
  definitionKey: string;
  definitionName: string;
  definitionVersion: number;
  formNameSnapshot?: null | string;
  lifecycleState?: null | number;
  modelId: number;
  publishedAt?: null | string;
  publishedByNameSnapshot?: null | string;
  startState?: null | number;
}

export interface BpmDefinitionDetailRecord extends BpmDefinitionRecord {
  categoryIdSnapshot?: null | number;
  compiledBpmnXml?: null | string;
  engineProcessDefinitionId?: null | string;
  formIdSnapshot?: null | number;
  formSchemaSnapshotJson?: null | string;
  formTypeSnapshot?: null | number;
  managerScopeSnapshotJson?: null | string;
  simpleModelSnapshotJson?: null | string;
  startRuleSnapshotJson?: null | string;
  summaryRuleSnapshotJson?: null | string;
  titleRuleSnapshotJson?: null | string;
  variableMappingSnapshotJson?: null | string;
}

export interface BpmDefinitionPageQueryParams {
  definitionKey?: string;
  definitionName?: string;
  lifecycleState?: null | number;
  pageNum: number;
  pageSize: number;
  startState?: null | number;
}

export async function queryBpmDefinitionPage(
  params: BpmDefinitionPageQueryParams,
) {
  return requestClient.post<PageResult<BpmDefinitionRecord>>(
    '/bpm/definition/query',
    {
      definitionKey: params.definitionKey?.trim() || undefined,
      definitionName: params.definitionName?.trim() || undefined,
      lifecycleState: params.lifecycleState ?? undefined,
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      startState: params.startState ?? undefined,
    },
  );
}

export async function getBpmDefinitionDetail(definitionId: number) {
  return requestClient.get<BpmDefinitionDetailRecord>(
    `/bpm/definition/detail/${definitionId}`,
  );
}
