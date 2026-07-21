import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const entryPath = resolve(
  process.cwd(),
  'apps/hunyuan-system/src/views/organization/directory/index.vue',
);
const featurePath = resolve(
  process.cwd(),
  'packages/features/organization/src/department-directory/index.vue',
);

describe('organization directory feature assembly', () => {
  it('keeps the app view as a thin feature adapter', () => {
    const source = readFileSync(entryPath, 'utf8');
    expect(source).toContain('@hunyuan/feature-organization/department-directory');
    expect(source).toContain('createOrganizationDepartmentClient(requestClient)');
    expect(source).not.toContain('ElTableColumn');
  });

  it('guards every write action with the stable capability code', () => {
    const source = readFileSync(featurePath, 'utf8');
    expect(source).toContain('organization.department.create');
    expect(source).toContain('organization.department.update');
    expect(source).toContain('organization.department.delete');
    expect(source).toContain('<AccessControl');
  });
});
