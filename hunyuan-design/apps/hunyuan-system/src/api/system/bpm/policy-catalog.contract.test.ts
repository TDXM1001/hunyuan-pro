import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const root = process.cwd();
const readSource = (path: string) => readFileSync(resolve(root, path), 'utf8');

describe('M2 policy catalog graph binding contract', () => {
  it('loads active typed policies and exposes selection-only graph bindings', () => {
    const apiSource = readSource('apps/hunyuan-system/src/api/system/bpm/policy.ts');
    const designerSource = readSource('apps/hunyuan-system/src/components/bpm/graph/graph-process-designer.vue');
    const editorSource = readSource('apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue');

    expect(apiSource).toContain('/bpm/policy-catalog/list');
    expect(apiSource).toContain('queryBpmPolicyCatalog');
    expect(apiSource).toContain('businessSummary');
    expect(apiSource).toContain('saveBpmPolicyVisualDraft');
    expect(apiSource).not.toContain('interface BpmPolicyCatalogRecord {\n  canonicalPayload');
    expect(designerSource).toContain('candidatePolicies');
    expect(designerSource).toContain('approvalPolicies');
    expect(designerSource).toContain('startVisibilityPolicies');
    expect(designerSource).toContain('function asText');
    expect(designerSource).toContain('function positiveInteger');
    expect(designerSource).not.toContain('placeholder="policyKey"');
    expect(editorSource).toContain('queryBpmPolicyCatalog');
  });
});
