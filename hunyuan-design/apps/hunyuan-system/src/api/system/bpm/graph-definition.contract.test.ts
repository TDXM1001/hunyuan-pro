import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

function readSource(path: string) {
  return readFileSync(resolve(process.cwd(), path), 'utf8');
}

describe('Graph definition inspection contracts', () => {
  it('exposes immutable definition inspection in the API and model editor', () => {
    const apiSource = readSource('apps/hunyuan-system/src/api/system/bpm/graph.ts');
    const editorSource = readSource('apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue');

    expect(apiSource).toContain('/bpm/graph-definition/detail/${versionId}');
    expect(apiSource).toContain('getBpmGraphDefinitionDetail');
    expect(apiSource).toContain('/bpm/graph-definition/latest-by-draft/${draftId}');
    expect(apiSource).toContain('getLatestBpmGraphDefinitionDetail');
    expect(editorSource).toContain('getBpmGraphDefinitionDetail');
    expect(editorSource).toContain('getLatestBpmGraphDefinitionDetail');
    expect(editorSource).toContain('publishedDefinition.value = latestDefinition');
    expect(editorSource).toContain('deactivateBpmGraphDefinition');
    expect(editorSource).toContain('definitionDetailVisible');
    expect(editorSource).toContain('compiledBpmnXml');
    expect(editorSource).toContain('mappings');
  });
});
