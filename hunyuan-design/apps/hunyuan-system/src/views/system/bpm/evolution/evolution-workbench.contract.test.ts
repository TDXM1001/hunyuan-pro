import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('BPM M8 迁移与演进工作台 contract', () => {
  it('覆盖版本 Diff、实例影响、映射预演、显式确认和逐实例审计', () => {
    const path = resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/evolution/workbench.vue');
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryGraphEvolutionDiff', 'queryAffectedInstances', 'previewBpmMigration',
      'executeBpmMigration', 'sourceVersionId', 'targetVersionId', 'nodeMappings',
      '布局变化', '语义变化', '阻断原因', '迁移预演', '确认迁移', '迁移审计',
      'compensationResult',
      '复核重试', '保留源版本', '登记补偿',
    ].forEach((needle) => expect(source).toContain(needle));
  });

  it('窄屏网格允许表格列收缩到视口内', () => {
    const path = resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/evolution/workbench.vue');
    const source = readFileSync(path, 'utf8');
    expect(source).toContain('@media (max-width: 1080px)');
    expect(source).toContain('grid-template-columns: minmax(0, 1fr);');
  });

  it('迁移审计对话框宽度受移动视口约束', () => {
    const path = resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/evolution/workbench.vue');
    const source = readFileSync(path, 'utf8');
    expect(source).toContain('width="min(720px, calc(100vw - 24px))"');
    expect(source).not.toContain('title="迁移审计" width="720px"');
  });
});
