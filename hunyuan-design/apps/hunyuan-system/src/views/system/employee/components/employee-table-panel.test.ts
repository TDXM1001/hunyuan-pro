import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const componentPath = resolve(
  process.cwd(),
  'apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue',
);

describe('employee-table-panel.vue', () => {
  it('renders a single keyword filter in the search panel', () => {
    const componentSource = readFileSync(componentPath, 'utf8');
    const keywordFilterCount = [
      ...componentSource.matchAll(
        /class="employee-table-panel__keyword-item" label="关键字"/g,
      ),
    ].length;

    expect(keywordFilterCount).toBe(1);
  });

  it('wires the batch department transfer action to the backend api', () => {
    const componentSource = readFileSync(componentPath, 'utf8');

    expect(componentSource).toContain('batchUpdateDepartment');
    expect(componentSource).toContain('批量转移部门');
    expect(componentSource).toContain('employeeIdList');
  });
});
