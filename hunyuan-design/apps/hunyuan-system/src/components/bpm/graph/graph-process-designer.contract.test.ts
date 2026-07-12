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
});
