import { requestClient } from '#/api/request';

export interface GraphEvolutionChange {
  description: string;
  elementId?: null | string;
  kind: string;
}

export interface GraphEvolutionDiffVO {
  changes: GraphEvolutionChange[];
  layoutChanged: boolean;
  migrationSuggested: boolean;
  semanticChanged: boolean;
  sourceVersionId: number;
  targetVersionId: number;
}

export interface BpmAffectedInstance {
  activeTaskCount: number;
  businessKey?: null | string;
  definitionKeySnapshot: string;
  graphDefinitionVersionId: number;
  instanceId: number;
  instanceNo: string;
  runState: number;
  startedAt: string;
  title: string;
}

export interface BpmMigrationItemVO {
  blockersJson: string;
  compensationResult?: null | string;
  dispositionByEmployeeId?: null | number;
  disposedAt?: null | string;
  engineCommandEvidenceJson?: null | string;
  failureReason?: null | string;
  instanceId: number;
  itemStatus: string;
  migratedAt?: null | string;
  migrationItemId: number;
  executedByEmployeeId?: null | number;
  sourceSnapshotJson: string;
  targetSnapshotJson?: null | string;
}

export interface BpmMigrationOperationItemVO {
  blockersJson: string;
  failureReason?: null | string;
  instanceId: number;
  itemStatus: string;
  migrationItemId: number;
}

export interface BpmMigrationOperationVO {
  batchCode: string;
  batchStatus: string;
  blockedCount: number;
  eligibleCount: number;
  failedCount: number;
  items: BpmMigrationOperationItemVO[];
  migrationBatchId: number;
  succeededCount: number;
  totalCount: number;
}

export interface BpmMigrationBatchDetailVO {
  actorEmployeeId: number;
  batchCode: string;
  batchStatus: string;
  blockedCount: number;
  completedAt?: null | string;
  confirmedAt?: null | string;
  confirmedByEmployeeId?: null | number;
  dataMappingJson: string;
  eligibleCount: number;
  failedCount: number;
  idempotencyKey: string;
  items: BpmMigrationItemVO[];
  mappingJson: string;
  migrationBatchId: number;
  previewedAt: string;
  reason: string;
  sourceVersionId: number;
  succeededCount: number;
  targetVersionId: number;
  totalCount: number;
}

export interface BpmMigrationPreviewParams {
  dataMappingJson: string;
  idempotencyKey: string;
  instanceIds: number[];
  nodeMappings: Record<string, string>;
  reason: string;
  sourceVersionId: number;
  targetVersionId: number;
}

export function queryGraphEvolutionDiff(sourceVersionId: number, targetVersionId: number) {
  return requestClient.get<GraphEvolutionDiffVO>('/bpm/evolution/diff', {
    params: { sourceVersionId, targetVersionId },
  });
}

export function queryAffectedInstances(sourceVersionId: number) {
  return requestClient.get<BpmAffectedInstance[]>('/bpm/evolution/affected', {
    params: { sourceVersionId },
  });
}

export function previewBpmMigration(data: BpmMigrationPreviewParams) {
  return requestClient.post<BpmMigrationOperationVO>('/bpm/evolution/migration/preview', data);
}

export function executeBpmMigration(batchId: number) {
  return requestClient.post<BpmMigrationOperationVO>(`/bpm/evolution/migration/${batchId}/execute`);
}

export function getBpmMigrationBatch(batchId: number) {
  return requestClient.get<BpmMigrationBatchDetailVO>(`/bpm/evolution/migration/${batchId}`);
}

export function disposeBpmMigrationItem(
  itemId: number,
  data: { action: 'COMPENSATED' | 'KEEP_SOURCE' | 'RETRY'; compensationResult?: string; reason: string },
) {
  return requestClient.post<BpmMigrationOperationVO>(
    `/bpm/evolution/migration/item/${itemId}/dispose`, data,
  );
}
