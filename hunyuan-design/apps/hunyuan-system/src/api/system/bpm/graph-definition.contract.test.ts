import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

function readSource(path: string) {
  return readFileSync(resolve(process.cwd(), path), 'utf8');
}

describe('Graph definition inspection contracts', () => {
  it('requires an enabled category selected by business name before creating a draft', () => {
    const editorSource = readSource('apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue');

    expect(editorSource).toContain('queryBpmCategoryPage');
    expect(editorSource).toContain('disabledFlag: false');
    expect(editorSource).toContain('请选择流程分类');
    expect(editorSource).toContain(':label="item.categoryName"');
    expect(editorSource).toContain(':value="item.categoryId"');
    expect(editorSource).not.toContain('分类 ID（可选）');
  });

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
