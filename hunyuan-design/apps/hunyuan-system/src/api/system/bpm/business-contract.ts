import { requestClient } from '#/api/request';

export type BpmBusinessContractLifecycleState = 'ACTIVE' | 'DRAFT' | 'RETIRED';

export interface BpmBusinessContractRecord {
  businessContractVersionId?: null | number;
  canonicalContractJson: string;
  catalogRevision: number;
  contractDigest: string;
  contractKey: string;
  contractVersion: number;
  lifecycleState: BpmBusinessContractLifecycleState;
  schemaVersion: number;
}

export interface BpmBusinessContractDraftParams {
  contractJson: string;
  contractKey: string;
  schemaVersion: number;
}

export interface BpmBusinessContractLifecycleParams {
  catalogRevision: number;
  contractKey: string;
  contractVersion: number;
}

export type BpmBusinessObjectFieldType = 'BOOLEAN' | 'DATE' | 'DATETIME' | 'DECIMAL' | 'EMPLOYEE_ID' | 'INTEGER' | 'STRING';
export type BpmBusinessObjectPerspective = 'APPLICANT' | 'APPROVER_EDIT' | 'APPROVER_READONLY';

export interface BpmBusinessObjectField {
  candidateUsable: boolean;
  key: string;
  label: string;
  presentation: { control: string; options: string[]; placeholder: string; unit: string };
  required: boolean;
  sensitivity: 'CONFIDENTIAL' | 'INTERNAL' | 'PUBLIC' | 'RESTRICTED';
  type: BpmBusinessObjectFieldType;
}

export interface BpmBusinessObjectDraft {
  attachmentRule: { allowedExtensions: string[]; maxCount: number; maxSizeMb: number; required: boolean };
  businessKeyRule: { datePattern: string; prefix: string; sequenceDigits: number };
  businessType: string;
  catalogRevision: number;
  contractKey: string;
  contractVersion: number;
  dataChangeRule: { editableFields: string[]; mode: 'FIELD_CONTROLLED' | 'LOCKED' };
  description?: string;
  fieldSchema: BpmBusinessObjectField[];
  lineItemSchema?: { fields: BpmBusinessObjectField[]; maxRows: number; minRows: number; name: string };
  objectName: string;
  routingFacts: BpmBusinessObjectField[];
  schemaVersion: 2;
  sourceSystem: string;
  workingDataSchema: BpmBusinessObjectField[];
}

export interface BpmBusinessObjectSummary {
  businessSummary: string;
  catalogRevision: number;
  contractKey: string;
  contractVersion: number;
  description?: string;
  lifecycleState: BpmBusinessContractLifecycleState;
  objectName: string;
  referenceCount: number;
  schemaVersion: number;
}

export interface BpmBusinessObjectDetail extends BpmBusinessObjectSummary {
  configuration?: BpmBusinessObjectDraft;
  findings: { code: string; fieldPath: string; message: string; severity: string; suggestion: string }[];
}

export interface BpmBusinessObjectTechnicalDetail {
  canonicalPayload: string;
  contractKey: string;
  contractVersion: number;
  digest: string;
  schemaVersion: number;
}

export interface BpmBusinessObjectReference {
  definitionVersion?: number;
  draftId?: number;
  graphDefinitionVersionId?: number;
  lifecycleState: string;
  processKey: string;
  processName: string;
  referenceSource: 'DRAFT' | 'PUBLISHED';
}

export interface BpmGenericApplicationSubmitParams {
  attachments: Record<string, any>[];
  businessKey: string;
  businessType: string;
  contractKey: string;
  contractVersion: number;
  fields: Record<string, any>;
  graphDefinitionVersionId: number;
  lineItems: Record<string, any>[];
  routingFacts: Record<string, any>;
  sourceSystem: string;
  summary?: null | string;
  title: string;
  workingData: Record<string, any>;
}

export interface BpmGenericApplicationSubmitResult {
  approvalSubjectSnapshotId: number;
  instanceId: number;
}

export async function queryBpmBusinessContracts(params?: {
  contractKey?: string;
  lifecycleState?: BpmBusinessContractLifecycleState;
}) {
  return requestClient.get<BpmBusinessObjectSummary[]>('/bpm/business-contract/list', {
    params,
  });
}

export async function getBpmBusinessObjectDetail(contractKey: string, contractVersion: number) {
  return requestClient.get<BpmBusinessObjectDetail>(`/bpm/business-contract/detail/${contractKey}/${contractVersion}`);
}

export async function createBpmBusinessObjectVisualDraft(params: BpmBusinessObjectDraft) {
  return requestClient.post<BpmBusinessObjectDetail>('/bpm/business-contract/visual-draft/create', params);
}

export async function saveBpmBusinessObjectVisualDraft(params: BpmBusinessObjectDraft) {
  return requestClient.post<BpmBusinessObjectDetail>('/bpm/business-contract/visual-draft/save', params);
}

export async function validateBpmBusinessObjectVisualDraft(params: BpmBusinessObjectDraft) {
  return requestClient.post<{ businessSummary: string; findings: BpmBusinessObjectDetail['findings']; valid: boolean }>('/bpm/business-contract/visual-draft/validate', params);
}

export async function deleteBpmBusinessObjectDraft(params: BpmBusinessContractLifecycleParams) {
  return requestClient.post('/bpm/business-contract/draft/delete', params);
}

export async function upgradeBpmBusinessObjectV1(params: { contractKey: string; contractVersion: number }) {
  return requestClient.post<BpmBusinessObjectDetail>('/bpm/business-contract/upgrade-v2', params);
}

export async function queryBpmBusinessObjectReferences(contractKey: string, contractVersion: number) {
  return requestClient.get<BpmBusinessObjectReference[]>(`/bpm/business-contract/references/${contractKey}/${contractVersion}`);
}

export async function getBpmBusinessObjectTechnicalDetail(contractKey: string, contractVersion: number) {
  return requestClient.get<BpmBusinessObjectTechnicalDetail>(`/bpm/business-contract/technical-detail/${contractKey}/${contractVersion}`);
}

export async function diffBpmBusinessObjectTechnicalDetail(contractKey: string, leftVersion: number, rightVersion: number) {
  return requestClient.post<{ changedFieldKeys: string[] }>('/bpm/business-contract/technical-diff', { contractKey, leftVersion, rightVersion });
}

export async function exportBpmBusinessObjectTechnicalDetail(contractKey: string, contractVersion: number) {
  return requestClient.get(`/bpm/business-contract/technical-export/${contractKey}/${contractVersion}`, { responseType: 'blob' });
}

export async function validateBpmBusinessContract(params: BpmBusinessContractDraftParams) {
  return requestClient.post<{ canonicalContractJson: string; contractDigest: string }>(
    '/bpm/business-contract/validate', params,
  );
}

export async function createBpmBusinessContractDraft(params: BpmBusinessContractDraftParams) {
  return requestClient.post<BpmBusinessContractRecord>('/bpm/business-contract/draft', params);
}

export async function copyBpmBusinessContractAsDraft(params: {
  contractKey: string;
  contractVersion: number;
}) {
  return requestClient.post<BpmBusinessContractRecord>('/bpm/business-contract/copy', params);
}

export async function activateBpmBusinessContract(params: BpmBusinessContractLifecycleParams) {
  return requestClient.post<BpmBusinessContractRecord>('/bpm/business-contract/activate', params);
}

export async function retireBpmBusinessContract(params: BpmBusinessContractLifecycleParams) {
  return requestClient.post<BpmBusinessContractRecord>('/bpm/business-contract/retire', params);
}

export async function queryGenericApplicationContracts() {
  return requestClient.get<BpmBusinessObjectSummary[]>('/app/bpm/generic-application/contracts');
}

export async function submitBpmGenericApplication(params: BpmGenericApplicationSubmitParams) {
  return requestClient.post<BpmGenericApplicationSubmitResult>(
    '/app/bpm/generic-application/submit', params,
  );
}
