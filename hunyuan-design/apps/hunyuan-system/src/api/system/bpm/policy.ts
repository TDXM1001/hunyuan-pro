import { requestClient } from '#/api/request';

export type BpmPolicyType = 'APPROVAL' | 'CANDIDATE' | 'START_VISIBILITY';

export interface BpmPolicyReference {
  policyKey: string;
  policyVersion: number;
  type: BpmPolicyType;
}

export interface BpmPolicyCatalogRecord {
  businessSummary: string;
  calculatedRiskLevel: 'HIGH' | 'LOW' | 'MEDIUM' | 'UNKNOWN';
  catalogRevision: number;
  description?: string;
  lifecycleState: 'ACTIVE' | 'DRAFT' | 'RETIRED';
  policyName: string;
  reference: BpmPolicyReference;
  referenceCount: number;
  schemaVersion: number;
}

export interface BpmIdentityReference { kind: 'DEPARTMENT' | 'EMPLOYEE' | 'POST' | 'ROLE' | 'USER_GROUP'; stableId: number; displayName?: string }
export interface CandidatePolicyVisualDocument { resolverType: string; identityReference?: BpmIdentityReference; resolutionPhase: string; memberOrder: string; emptyCandidatePolicy: string; selfApprovalPolicy: string; fallbackIdentityReference?: BpmIdentityReference }
export interface ApprovalPolicyVisualDocument { completionMode: string; ratioPercent?: number; rejectionRule: string; returnRule: string; allowedActions: string[] }
export interface PolicyScopeVisualDocument { type: string; identities?: BpmIdentityReference[]; scopes?: PolicyScopeVisualDocument[] }
export interface StartVisibilityPolicyVisualDocument { startScope: PolicyScopeVisualDocument; visibilityScope: PolicyScopeVisualDocument }
export interface BpmPolicyVisualDraft { type: BpmPolicyType; policyKey: string; policyName: string; description?: string; schemaVersion: 2; policyVersion: number; catalogRevision: number; candidate?: CandidatePolicyVisualDocument; approval?: ApprovalPolicyVisualDocument; startVisibility?: StartVisibilityPolicyVisualDocument }
export interface BpmPolicyFinding { code: string; severity: string; fieldPath: string; message: string; suggestion: string }
export interface BpmIdentityOption { kind: string; stableId: number; displayName: string; departmentId?: number; departmentName?: string; disabled: boolean }
export interface BpmIdentityOptionPage { items: BpmIdentityOption[]; total: number; pageNum: number; pageSize: number }
export interface BpmPolicyBusinessDetail extends BpmPolicyCatalogRecord { configuration?: BpmPolicyVisualDraft; findings: BpmPolicyFinding[] }
export interface BpmPolicyTechnicalDetail { reference: BpmPolicyReference; schemaVersion: number; canonicalPayload: string; digest: string; diagnostics: BpmPolicyFinding[] }
export interface BpmPolicyTechnicalDiff { left: BpmPolicyReference; right: BpmPolicyReference; changedPaths: string[] }
export interface BpmDefinitionReference { graphDefinitionVersionId?: number; draftId?: number; referenceSource: 'DRAFT' | 'PUBLISHED'; processKey: string; processName: string; definitionVersion: number; lifecycleState: string }
export interface BpmPolicySimulationResult { resolvedMembers: Array<{ employeeId: number; employeeName: string; departmentName?: string }>; diagnostics: string[]; automaticOutcome?: string; businessSummary: string; findings: BpmPolicyFinding[] }
export interface BpmPolicyBusinessValidationResult { valid: boolean; calculatedRiskLevel: 'HIGH' | 'LOW' | 'MEDIUM'; businessSummary: string; findings: BpmPolicyFinding[] }

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
  return requestClient.get<BpmPolicyBusinessDetail>(
    `/bpm/policy-catalog/detail/${reference.type}/${encodeURIComponent(reference.policyKey)}/${reference.policyVersion}`,
  );
}

export function saveBpmPolicyVisualDraft(params: BpmPolicyVisualDraft) { return requestClient.post<BpmPolicyBusinessDetail>('/bpm/policy-catalog/visual-draft/save', params); }
export function createBpmPolicyVisualDraft(params: BpmPolicyVisualDraft) { return requestClient.post<BpmPolicyBusinessDetail>('/bpm/policy-catalog/visual-draft/create', params); }
export function validateBpmPolicyVisualDraft(params: BpmPolicyVisualDraft) { return requestClient.post<BpmPolicyBusinessValidationResult>('/bpm/policy-catalog/visual-draft/validate', params); }
export function queryBpmIdentityOptions(params: { kind: string; keyword?: string; departmentId?: number; stableId?: number; pageNum?: number; pageSize?: number }) { return requestClient.get<BpmIdentityOptionPage>('/bpm/policy-catalog/identity-options', { params }); }
export function simulateBpmPolicy(params: { draft: BpmPolicyVisualDraft; starterEmployeeId: number }) { return requestClient.post<BpmPolicySimulationResult>('/bpm/policy-catalog/simulate', params); }

export function validateBpmPolicyDraft(params: CreateBpmPolicyDraftParams) {
  return requestClient.post<{ canonicalPayload: string; digest: string; schemaVersion: number }>(
    '/bpm/policy-catalog/validate',
    params,
  );
}

export function deleteBpmPolicyDraft(params: ChangeBpmPolicyLifecycleParams) { return requestClient.post('/bpm/policy-catalog/draft/delete', params); }
export function activateHighRiskBpmPolicyVersion(params: ChangeBpmPolicyLifecycleParams & { confirmationReason: string }) { return requestClient.post('/bpm/policy-catalog/activate-high-risk', params); }
export function getBpmPolicyTechnicalDetail(reference: BpmPolicyReference) { return requestClient.get<BpmPolicyTechnicalDetail>(`/bpm/policy-catalog/technical-detail/${reference.type}/${encodeURIComponent(reference.policyKey)}/${reference.policyVersion}`); }
export function diffBpmPolicyTechnicalDetail(left:BpmPolicyReference,right:BpmPolicyReference){return requestClient.post<BpmPolicyTechnicalDiff>('/bpm/policy-catalog/technical-diff',{leftType:left.type,leftPolicyKey:left.policyKey,leftPolicyVersion:left.policyVersion,rightType:right.type,rightPolicyKey:right.policyKey,rightPolicyVersion:right.policyVersion})}
export function exportBpmPolicyTechnicalDetail(reference: BpmPolicyReference) { return requestClient.get(`/bpm/policy-catalog/technical-export/${reference.type}/${encodeURIComponent(reference.policyKey)}/${reference.policyVersion}`, { responseType: 'blob' }); }
export function queryBpmPolicyReferences(reference:BpmPolicyReference){return requestClient.get<BpmDefinitionReference[]>(`/bpm/policy-catalog/references/${reference.type}/${encodeURIComponent(reference.policyKey)}/${reference.policyVersion}`)}

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
