import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const categoryPagePath =
  'apps/hunyuan-system/src/views/system/bpm/category/category-list.vue';
const formDesignerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/form/form-designer.vue';
const modelEditorPath =
  'apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue';
const definitionPagePath =
  'apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue';
const instancePagePath =
  'apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue';
const taskPagePath =
  'apps/hunyuan-system/src/views/system/bpm/task/task-list.vue';
const listenerPagePath =
  'apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue';
const apiPath = 'apps/hunyuan-system/src/api/system/bpm/index.ts';
const bpmRoutePath = 'apps/hunyuan-system/src/router/routes/static/bpm.ts';

describe('bpm 后端菜单对接页面', () => {
  it('提供真实 BPM 管理页面而不是桥接占位页', () => {
    [
      categoryPagePath,
      formDesignerPagePath,
      modelEditorPath,
      definitionPagePath,
      instancePagePath,
      taskPagePath,
      listenerPagePath,
    ].forEach((path) => {
      expect(existsSync(resolve(process.cwd(), path))).toBe(true);
    });
  });

  it('保持 BPM 列表页紧凑并直接路由到本地页面', () => {
    const categorySource = readFileSync(
      resolve(process.cwd(), categoryPagePath),
      'utf8',
    );

    expect(categorySource).toContain('ArtSearchPanel');
    expect(categorySource).toContain('ArtTablePanel');
    expect(categorySource).toContain(':collapsible="false"');
    expect(categorySource).not.toContain('category-page__title');
    expect(categorySource).not.toContain('module-bridge');
  });

  it('为表单和模型提供隐藏设计器路由页', () => {
    [formDesignerPagePath, modelEditorPath].forEach((path) => {
      expect(existsSync(resolve(process.cwd(), path))).toBe(true);
    });

    const routeSource = readFileSync(resolve(process.cwd(), bpmRoutePath), 'utf8');

    expect(routeSource).toContain('/system/bpm/form/designer');
    expect(routeSource).toContain('/system/bpm/model/designer');
    expect(routeSource).toContain('hideInMenu: true');
  });

  it('让 BPM 管理面覆盖分类、定义、实例、任务、监听器五个列表入口', () => {
    [
      categoryPagePath,
      definitionPagePath,
      instancePagePath,
      taskPagePath,
      listenerPagePath,
    ].forEach((path) => {
      const source = readFileSync(resolve(process.cwd(), path), 'utf8');

      expect(source).toContain('ArtSearchPanel');
      expect(source).toContain('ArtTablePanel');
      expect(source).toContain('ArtTableHeader');
      expect(source).toContain('ArtTable');
    });
  });

  it('让新的表单设计页继续沿用现有编辑页基座', () => {
    const formDesignerSource = readFileSync(
      resolve(process.cwd(), formDesignerPagePath),
      'utf8',
    );

    expect(formDesignerSource).toContain('ArtEditPage');
    expect(formDesignerSource).toContain('ArtEditSection');
    expect(formDesignerSource).toContain('SystemBpmFormDesigner');
  });

  it('让流程模型编辑页沿用现有编辑页基座', () => {
    const modelEditorSource = readFileSync(
      resolve(process.cwd(), modelEditorPath),
      'utf8',
    );

    expect(modelEditorSource).toContain('ArtEditPage');
    expect(modelEditorSource).toContain('ArtEditSection');
  });

  it('把 BPM API barrel 对齐到分类、表单、模型、定义、运行时与监听器 contract', () => {
    const source = readFileSync(resolve(process.cwd(), apiPath), 'utf8');

    expect(source).toContain('/bpm/category/');
    expect(source).toContain('/bpm/form/');
    expect(source).toContain('/bpm/model/');
    expect(source).toContain('/bpm/definition/');
    expect(source).toContain('/bpm/designer/');
    expect(source).toContain('/bpm/task/');
    expect(source).toContain('/bpm/listener/');
  });
});
