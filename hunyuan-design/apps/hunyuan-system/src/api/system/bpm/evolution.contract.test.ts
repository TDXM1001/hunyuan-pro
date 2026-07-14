import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('BPM M8 迁移与演进 API contract', () => {
  it('暴露 Diff、影响分析、预演、确认和审计接口', () => {
    const path = resolve(process.cwd(), 'apps/hunyuan-system/src/api/system/bpm/evolution.ts');
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryGraphEvolutionDiff', 'queryAffectedInstances', 'previewBpmMigration',
      'executeBpmMigration', 'getBpmMigrationBatch', 'disposeBpmMigrationItem', '/bpm/evolution/diff',
      '/bpm/evolution/affected', '/bpm/evolution/migration/preview',
      '/bpm/evolution/migration/',
    ].forEach((needle) => expect(source).toContain(needle));
  });
});
