import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('BPM M7 运营治理工作台 contract', () => {
  it('覆盖异常检索、授权处置、SLA 指标和保留评估', () => {
    const path = resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/operations/workbench.vue',
    );
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryBpmOperationsCasePage',
      'executeBpmOperationsAction',
      'queryBpmOperationsMetrics',
      'evaluateBpmOperationsRetention',
      'businessKey',
      'graphDefinitionVersionId',
      'assigneeEmployeeId',
      'slaLevel',
      'failureCode',
      'eventId',
      '重试',
      '补偿',
      '终止',
      '归档评估',
      '处置审计',
      '导出',
    ].forEach((needle) => expect(source).toContain(needle));
  });
});
