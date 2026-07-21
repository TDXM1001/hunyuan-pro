import type { RequestClient } from '@vben/request';

import { describe, expect, it, vi } from 'vitest';

import { organizationFeature } from '../index';
import { createOrganizationDepartmentClient } from './client';

function createRequestMock() {
  return {
    delete: vi.fn().mockResolvedValue(undefined),
    get: vi.fn().mockResolvedValue([]),
    post: vi.fn().mockResolvedValue(101),
    put: vi.fn().mockResolvedValue(undefined),
  } as unknown as RequestClient;
}

describe('organization department generated client adapter', () => {
  it('uses the versioned admin api contract', async () => {
    const request = createRequestMock();
    const client = createOrganizationDepartmentClient(request);

    await client.list();
    await client.listManagers();
    await client.delete(23);

    expect(request.get).toHaveBeenNthCalledWith(
      1,
      '/admin/v1/organization/departments',
    );
    expect(request.get).toHaveBeenNthCalledWith(
      2,
      '/admin/v1/organization/departments/manager-options',
    );
    expect(request.delete).toHaveBeenCalledWith(
      '/admin/v1/organization/departments/23',
    );
  });

  it('normalizes create and update payloads', async () => {
    const request = createRequestMock();
    const client = createOrganizationDepartmentClient(request);
    const command = {
      departmentName: '  研发中心  ',
      managerId: undefined,
      parentId: 0,
      sort: 10,
    };

    await client.create(command);
    await client.update(9, command);

    const expected = {
      departmentName: '研发中心',
      managerId: null,
      parentId: 0,
      sort: 10,
    };
    expect(request.post).toHaveBeenCalledWith(
      '/admin/v1/organization/departments',
      expected,
    );
    expect(request.put).toHaveBeenCalledWith(
      '/admin/v1/organization/departments/9',
      expected,
    );
  });

  it('declares the stable module route and capabilities', () => {
    expect(organizationFeature.id).toBe('organization.directory');
    expect(organizationFeature.route.path).toBe('/organization/directory');
    expect(organizationFeature.capabilities).toEqual([
      'organization.department.read',
      'organization.department.create',
      'organization.department.update',
      'organization.department.delete',
    ]);
  });
});
