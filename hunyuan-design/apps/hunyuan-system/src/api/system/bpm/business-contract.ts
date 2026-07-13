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

export interface BpmGenericApplicationSubmitParams {
  attachmentsJson: string;
  businessKey: string;
  businessType: string;
  contractKey: string;
  contractVersion: number;
  fieldsJson: string;
  graphDefinitionVersionId: number;
  lineItemsJson: string;
  routingFactsJson: string;
  sourceSystem: string;
  summary?: null | string;
  title: string;
  workingDataJson: string;
}

export interface BpmGenericApplicationSubmitResult {
  approvalSubjectSnapshotId: number;
  instanceId: number;
}

export async function queryBpmBusinessContracts(params?: {
  contractKey?: string;
  lifecycleState?: BpmBusinessContractLifecycleState;
}) {
  return requestClient.get<BpmBusinessContractRecord[]>('/bpm/business-contract/list', {
    params,
  });
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
  return requestClient.get<BpmBusinessContractRecord[]>('/app/bpm/generic-application/contracts');
}

export async function submitBpmGenericApplication(params: BpmGenericApplicationSubmitParams) {
  return requestClient.post<BpmGenericApplicationSubmitResult>(
    '/app/bpm/generic-application/submit', params,
  );
}
