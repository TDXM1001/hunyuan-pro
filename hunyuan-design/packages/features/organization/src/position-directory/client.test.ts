import type { RequestClient } from '@vben/request';

import { describe, expect, it, vi } from 'vitest';

import { organizationPositionFeature } from '../index';
import { createOrganizationPositionClient } from './client';

function createRequestMock() {
  return {
    delete: vi.fn().mockResolvedValue(undefined),
    get: vi.fn().mockResolvedValue([]),
    post: vi.fn().mockResolvedValue(101),
    put: vi.fn().mockResolvedValue(undefined),
  } as unknown as RequestClient;
}

describe('组织岗位稳定客户端契约', () => {
  it('只使用版本化岗位管理接口', async () => {
    const request = createRequestMock();
    const client = createOrganizationPositionClient(request);

    await client.list();
    await client.get(7);
    await client.delete(7);

    expect(request.get).toHaveBeenNthCalledWith(
      1,
      '/admin/v1/organization/positions',
    );
    expect(request.get).toHaveBeenNthCalledWith(
      2,
      '/admin/v1/organization/positions/7',
    );
    expect(request.delete).toHaveBeenCalledWith(
      '/admin/v1/organization/positions/7',
    );
  });

  it('规范化岗位新增和更新请求体', async () => {
    const request = createRequestMock();
    const client = createOrganizationPositionClient(request);
    const command = {
      positionLevel: ' P7 ',
      positionName: ' 高级工程师 ',
      remark: ' 核心岗位 ',
      sort: 10,
    };

    await client.create(command);
    await client.update(7, command);

    const expected = {
      positionLevel: 'P7',
      positionName: '高级工程师',
      remark: '核心岗位',
      sort: 10,
    };
    expect(request.post).toHaveBeenCalledWith(
      '/admin/v1/organization/positions',
      expected,
    );
    expect(request.put).toHaveBeenCalledWith(
      '/admin/v1/organization/positions/7',
      expected,
    );
  });

  it('声明岗位路由和四个稳定能力码', () => {
    expect(organizationPositionFeature).toEqual({
      capabilities: [
        'organization.position.read',
        'organization.position.create',
        'organization.position.update',
        'organization.position.delete',
      ],
      id: 'organization.position',
      route: {
        component: '/system/position/position-list.vue',
        path: '/organization/position',
      },
    });
  });
});
