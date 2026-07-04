import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const configPagePath = 'apps/hunyuan-system/src/views/support/config/config-list.vue';
const configApiPath = 'apps/hunyuan-system/src/api/system/config.ts';
const dictPagePath = 'apps/hunyuan-system/src/views/support/dict/index.vue';
const dictApiPath = 'apps/hunyuan-system/src/api/system/dict.ts';

describe('system settings support modules', () => {
  it('provides a real parameter config page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), configPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportConfigList');
  });

  it('keeps the parameter config page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), configPagePath), 'utf8');

    expect(source).not.toContain('config-page__title');
    expect(source).not.toContain('config-page__hero');
    expect(source).not.toContain('config-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires the parameter config api module to the backend config endpoints', () => {
    const apiPath = resolve(process.cwd(), configApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/config/query'");
    expect(source).toContain("'/config/add'");
    expect(source).toContain("'/config/update'");
    expect(source).toContain('buildConfigPageQueryPayload');
    expect(source).toContain('buildConfigMutationPayload');
  });

  it('surfaces the config key, name, value, and remark fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), configPagePath), 'utf8');

    expect(source).toContain('configKey');
    expect(source).toContain('configName');
    expect(source).toContain('configValue');
    expect(source).toContain('remark');
  });

  it('provides a real dictionary management page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), dictPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportDictIndex');
  });

  it('keeps the dictionary page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), dictPagePath), 'utf8');

    expect(source).not.toContain('dict-page__title');
    expect(source).not.toContain('dict-page__hero');
    expect(source).not.toContain('dict-page__desc');
    expect(source).toContain(':collapsible="false"');
    expect(source).toContain('grid-template-columns: 320px minmax(0, 1fr);');
  });

  it('wires the dictionary api module to the backend dict and dictData endpoints', () => {
    const apiPath = resolve(process.cwd(), dictApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/dict/queryPage'");
    expect(source).toContain("'/dict/add'");
    expect(source).toContain("'/dict/update'");
    expect(source).toContain('/dict/dictData/queryDictData/${dictId}');
    expect(source).toContain("'/dict/dictData/add'");
    expect(source).toContain("'/dict/dictData/update'");
    expect(source).toContain('buildDictPageQueryPayload');
    expect(source).toContain('buildDictMutationPayload');
    expect(source).toContain('buildDictDataMutationPayload');
  });

  it('surfaces dictionary and dictionary-item key fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), dictPagePath), 'utf8');

    expect(source).toContain('dictName');
    expect(source).toContain('dictCode');
    expect(source).toContain('dataLabel');
    expect(source).toContain('dataValue');
    expect(source).toContain('dataStyle');
    expect(source).toContain('disabledFlag');
  });
});
