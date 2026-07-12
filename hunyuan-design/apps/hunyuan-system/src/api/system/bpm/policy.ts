import { requestClient } from '#/api/request';

export type BpmPolicyType = 'APPROVAL' | 'CANDIDATE' | 'START_VISIBILITY';

export interface BpmPolicyReference {
  policyKey: string;
  policyVersion: number;
  type: BpmPolicyType;
}

export interface BpmPolicyCatalogRecord {
  canonicalPayload: string;
  catalogRevision: number;
  digest: string;
  lifecycleState: 'ACTIVE' | 'DRAFT' | 'RETIRED';
  reference: BpmPolicyReference;
  schemaVersion: number;
}

export interface QueryBpmPolicyCatalogParams {
  lifecycleState?: BpmPolicyCatalogRecord['lifecycleState'];
  policyKey?: string;
  type: BpmPolicyType;
}

export interface CreateBpmPolicyDraftParams {
  policyJson: string;
  policyKey: string;
  schemaVersion: number;
  type: BpmPolicyType;
}

export interface ChangeBpmPolicyLifecycleParams extends BpmPolicyReference {
  catalogRevision: number;
}

export function queryBpmPolicyCatalog(params: QueryBpmPolicyCatalogParams) {
  return requestClient.get<BpmPolicyCatalogRecord[]>('/bpm/policy-catalog/list', { params });
}

export function getBpmPolicyCatalogVersion(reference: BpmPolicyReference) {
  return requestClient.get<BpmPolicyCatalogRecord>(
    `/bpm/policy-catalog/detail/${reference.type}/${encodeURIComponent(reference.policyKey)}/${reference.policyVersion}`,
  );
}

export function validateBpmPolicyDraft(params: CreateBpmPolicyDraftParams) {
  return requestClient.post<Pick<BpmPolicyCatalogRecord, 'canonicalPayload' | 'digest' | 'schemaVersion'>>(
    '/bpm/policy-catalog/validate',
    params,
  );
}

export function createBpmPolicyDraft(params: CreateBpmPolicyDraftParams) {
  return requestClient.post<BpmPolicyCatalogRecord>('/bpm/policy-catalog/draft', params);
}

export function copyBpmPolicyAsDraft(reference: BpmPolicyReference) {
  return requestClient.post<BpmPolicyCatalogRecord>('/bpm/policy-catalog/copy', reference);
}

export function activateBpmPolicyVersion(params: ChangeBpmPolicyLifecycleParams) {
  return requestClient.post<BpmPolicyCatalogRecord>('/bpm/policy-catalog/activate', params);
}

export function retireBpmPolicyVersion(params: ChangeBpmPolicyLifecycleParams) {
  return requestClient.post<BpmPolicyCatalogRecord>('/bpm/policy-catalog/retire', params);
}
