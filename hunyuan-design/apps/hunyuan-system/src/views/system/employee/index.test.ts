import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const entryPath = resolve(
  process.cwd(),
  'apps/hunyuan-system/src/views/system/employee/index.vue',
);
const featurePagePath = resolve(
  process.cwd(),
  'packages/features/identity-employee/src/employee/index.vue',
);

describe('employee feature assembly and permission boundary', () => {
  it('keeps the application route as a thin dependency assembly', () => {
    const entrySource = readFileSync(entryPath, 'utf8');

    expect(entrySource).toContain("defineOptions({ name: 'SystemEmployeeIndex' })");
    expect(entrySource).toContain('createIdentityEmployeeClient(requestClient)');
    expect(entrySource).toContain('employeeDepartmentProviderKey');
    expect(entrySource).toContain('employeePositionProviderKey');
    expect(entrySource).toContain('<IdentityEmployeePage />');
    expect(entrySource).not.toContain('useAccess');
    expect(entrySource).not.toContain('EmployeeTablePanel');
  });

  it('loads and renders the department directory only with its read capability', () => {
    const featureSource = readFileSync(featurePagePath, 'utf8');

    expect(featureSource).toContain(
      "hasAccessByCodes(['organization.department.read'])",
    );
    expect(featureSource).toContain('canReadDepartments.value');
    expect(featureSource).toContain('v-if="canReadDepartments"');
    expect(featureSource).toContain(
      ':show-department-filter="canReadDepartments"',
    );
  });
});
