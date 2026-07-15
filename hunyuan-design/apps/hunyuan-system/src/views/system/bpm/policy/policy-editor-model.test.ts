import { describe, expect, it } from 'vitest';

import {
  createPolicyModel,
  createRoleCandidateModel,
  toPolicyVisualSaveParams,
} from './policy-editor-model';

describe('policy visual editor model', () => {
  it.each(['CANDIDATE', 'APPROVAL', 'START_VISIBILITY'] as const)(
    'creates %s visual drafts with Schema v2',
    (type) => {
      const model = createPolicyModel(type);

      expect(model.schemaVersion).toBe(2);
      expect(toPolicyVisualSaveParams(model).schemaVersion).toBe(2);
    },
  );

  it('builds a role candidate request without editable JSON or client risk', () => {
    const model = createRoleCandidateModel();
    model.candidate!.identityReference = { kind: 'ROLE', stableId: 8, displayName: '财务经理' };
    const params = toPolicyVisualSaveParams(model);
    expect(params.candidate?.identityReference).toEqual({ kind: 'ROLE', stableId: 8, displayName: '财务经理' });
    expect(params).not.toHaveProperty('policyJson');
    expect(params).not.toHaveProperty('riskLevel');
    expect(params.candidate).not.toHaveProperty('clientRiskLevel');
  });
});
