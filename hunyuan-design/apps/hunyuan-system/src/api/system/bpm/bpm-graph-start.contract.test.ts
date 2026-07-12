import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const root = process.cwd();
const readSource = (path: string) => readFileSync(resolve(root, path), 'utf8');

describe('M2 Graph start entry contract', () => {
  it('uses the existing start list and form for an exact Graph definition version', () => {
    const runtimeApi = readSource('apps/hunyuan-system/src/api/system/bpm/runtime.ts');
    const startableList = readSource('apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue');
    const startForm = readSource('apps/hunyuan-system/src/views/system/bpm/runtime/start-form.vue');

    expect(runtimeApi).toContain('graphDefinitionVersionId');
    expect(runtimeApi).toContain('getBpmGraphStartDraft');
    expect(startableList).toContain('definitionSource');
    expect(startableList).toContain('graphDefinitionVersionId');
    expect(startForm).toContain('routeGraphDefinitionVersionId');
    expect(startForm).toContain('getBpmGraphStartDraft');
    expect(startForm).toContain('graphDefinitionVersionId: formState.graphDefinitionVersionId');
  });
});
