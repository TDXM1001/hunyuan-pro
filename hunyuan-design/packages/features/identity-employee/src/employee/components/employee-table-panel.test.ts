import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const componentPath = resolve(
  process.cwd(),
  'packages/features/identity-employee/src/employee/components/employee-table-panel.vue',
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

  it('wires employee actions to the injected client and capability codes', () => {
    const componentSource = readFileSync(componentPath, 'utf8');

    expect(componentSource).toContain('client.assignDepartment');
    expect(componentSource).toContain('批量转移部门');
    expect(componentSource).toContain('employeeIds');
    expect(componentSource).toContain('client.resetPassword');
    expect(componentSource).toContain('identity.employee.create');
    expect(componentSource).toContain('identity.employee.update');
    expect(componentSource).toContain('identity.employee.enable');
    expect(componentSource).toContain('identity.employee.disable');
    expect(componentSource).toContain('identity.employee.department.assign');
    expect(componentSource).toContain('identity.employee.delete');
    expect(componentSource).toContain('identity.employee.password.reset');
  });
});
