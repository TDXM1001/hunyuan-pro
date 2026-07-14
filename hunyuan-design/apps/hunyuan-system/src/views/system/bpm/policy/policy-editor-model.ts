import type { BpmPolicyVisualDraft } from '#/api/system/bpm/policy';

export function createRoleCandidateModel(): BpmPolicyVisualDraft {
  return { type: 'CANDIDATE', policyKey: '', policyName: '', policyVersion: 1, catalogRevision: 0, candidate: { resolverType: 'ROLE', resolutionPhase: 'ACTIVATE', memberOrder: 'SELECTION_ORDER', emptyCandidatePolicy: 'BLOCK', selfApprovalPolicy: 'BLOCK' } };
}

export function createPolicyModel(type: BpmPolicyVisualDraft['type']): BpmPolicyVisualDraft {
  const base = { type, policyKey: '', policyName: '', policyVersion: 1, catalogRevision: 0 };
  if (type === 'APPROVAL') return { ...base, approval: { completionMode: 'ALL', ratioPercent: 100, rejectionRule: 'IMMEDIATE', returnRule: 'RETURN_INITIATOR', allowedActions: ['APPROVE', 'REJECT', 'RETURN'] } };
  if (type === 'START_VISIBILITY') return { ...base, startVisibility: { startScope: { type: 'ALL' }, visibilityScope: { type: 'ALL' } } };
  return createRoleCandidateModel();
}

export function toPolicyVisualSaveParams(model: BpmPolicyVisualDraft): BpmPolicyVisualDraft {
  const candidate = model.candidate ? { ...model.candidate } : undefined;
  return { ...model, candidate };
}
