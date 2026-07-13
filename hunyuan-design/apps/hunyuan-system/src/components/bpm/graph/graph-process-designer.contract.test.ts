import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const componentPath = resolve(
  process.cwd(),
  'apps/hunyuan-system/src/components/bpm/graph/graph-process-designer.vue',
);

describe('graph process designer contract', () => {
  it('exposes gateway pairing and editable graph connections', () => {
    const source = readFileSync(componentPath, 'utf8');

    expect(source).toContain('gatewayMode');
    expect(source).toContain('pairedGatewayId');
    expect(source).toContain('createConnection');
    expect(source).toContain('removeSelectedEdge');
    expect(source).toContain('selectedEdgeId');
    expect(source).toContain('routeCondition');
  });

  it('exposes controlled advanced runtime node configuration without endpoint or credential inputs', () => {
    const source = readFileSync(componentPath, 'utf8');

    expect(source).toContain("type: 'DELAY'");
    expect(source).toContain("type: 'EXTERNAL_TRIGGER'");
    expect(source).toContain("type: 'SUB_PROCESS'");
    expect(source).toContain('connectorVersion');
    expect(source).toContain('timeoutPolicy');
    expect(source).toContain('calledDefinitionVersionId');
    expect(source).toContain('cancelPropagation');
    expect(source).not.toContain('baseEndpoint');
    expect(source).not.toContain('credentialRef');
  });
});
