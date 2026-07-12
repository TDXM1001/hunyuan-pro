import { requestClient } from '#/api/request';
import type { ProcessDefinitionGraph } from '#/components/bpm/graph/graph-process-model';

export interface BpmGraphDraftRecord {
  draftId: number;
  graphJson: string;
  layoutJson: string;
  revision: number;
  semanticHash: string;
}

export interface CreateBpmGraphDraftParams {
  categoryId?: number;
  graph: ProcessDefinitionGraph;
  processKey: string;
  processName: string;
}

export interface SaveBpmGraphDraftParams {
  draftId: number;
  graph: ProcessDefinitionGraph;
  revision: number;
}

export interface BpmGraphDefinitionElementMappingRecord {
  authoredElementId: string;
  authoredElementKind: string;
  compiledElementId: string;
  compiledElementType: string;
}

export interface BpmGraphDefinitionDetailRecord {
  compiledBpmnXml: string;
  compilerVersion: string;
  definitionVersion: number;
  dependencyVersionsJson: string;
  deploymentId: string;
  engineProcessDefinitionId: string;
  graphDefinitionVersionId: number;
  graphSnapshotJson: string;
  layoutSnapshotJson: string;
  lifecycleState: string;
  mappings: BpmGraphDefinitionElementMappingRecord[];
  processKey: string;
  semanticHash: string;
}

export function createBpmGraphDraft(params: CreateBpmGraphDraftParams) {
  return requestClient.post<BpmGraphDraftRecord>('/bpm/graph-draft/create', params);
}

export function getBpmGraphDraft(draftId: number) {
  return requestClient.get<BpmGraphDraftRecord>(`/bpm/graph-draft/detail/${draftId}`);
}

export function saveBpmGraphDraft(params: SaveBpmGraphDraftParams) {
  return requestClient.post<BpmGraphDraftRecord>('/bpm/graph-draft/save', params);
}

export function publishBpmGraphDefinition(draftId: number) {
  return requestClient.post<number>('/bpm/graph-definition/publish', { draftId });
}

export function deactivateBpmGraphDefinition(versionId: number) {
  return requestClient.post<string>(`/bpm/graph-definition/deactivate/${versionId}`);
}

export function getBpmGraphDefinitionDetail(versionId: number) {
  return requestClient.get<BpmGraphDefinitionDetailRecord>(
    `/bpm/graph-definition/detail/${versionId}`,
  );
}

export function getLatestBpmGraphDefinitionDetail(draftId: number) {
  return requestClient.get<BpmGraphDefinitionDetailRecord | undefined>(
    `/bpm/graph-definition/latest-by-draft/${draftId}`,
  );
}

export function restoreBpmGraphDraft(record: BpmGraphDraftRecord): ProcessDefinitionGraph {
  const graph = JSON.parse(record.graphJson) as ProcessDefinitionGraph;
  const layouts = JSON.parse(record.layoutJson || '{}') as Record<string, Record<string, number>>;
  return {
    ...graph,
    nodes: graph.nodes.map((node) => ({ ...node, layout: layouts[node.nodeId] || {} })),
  };
}
