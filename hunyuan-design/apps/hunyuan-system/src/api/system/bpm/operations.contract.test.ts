import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('BPM M7 运营治理 API contract', () => {
  it('暴露统一异常队列、治理动作、指标和保留接口', () => {
    const path = resolve(
      process.cwd(),
      'apps/hunyuan-system/src/api/system/bpm/operations.ts',
    );
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryBpmOperationsCasePage',
      'executeBpmOperationsAction',
      'queryBpmOperationsMetrics',
      'evaluateBpmOperationsRetention',
      'getBpmOperationsCaseDetail',
      'exportBpmOperationsCases',
      '/bpm/operations/case/query',
      '/bpm/operations/action/',
      '/bpm/operations/metrics/query',
      '/bpm/operations/retention/evaluate',
      '/bpm/operations/case/export',
    ].forEach((needle) => expect(source).toContain(needle));
  });
});
