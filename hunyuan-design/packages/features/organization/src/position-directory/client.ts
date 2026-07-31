import type { RequestClient } from '@vben/request';

import type {
  OrganizationPositionClient,
  PositionCommand,
  PositionRecord,
} from './contract';

const BASE_PATH = '/admin/v1/organization/positions';

// 岗位目录的删除和查询均使用同一稳定 ID 路径，避免页面自行拼接历史接口。
/** 创建岗位目录客户端，统一岗位记录的查询、新增、修改和删除入口。 */
export function createOrganizationPositionClient(
  requestClient: RequestClient,
): OrganizationPositionClient {
  return {
    create(command) {
      return requestClient.post<number>(BASE_PATH, normalize(command));
    },
    async delete(positionId) {
      await requestClient.delete(`${BASE_PATH}/${positionId}`);
    },
    get(positionId) {
      return requestClient.get<PositionRecord>(`${BASE_PATH}/${positionId}`);
    },
    list() {
      return requestClient.get<PositionRecord[]>(BASE_PATH);
    },
    async update(positionId, command) {
      await requestClient.put(
        `${BASE_PATH}/${positionId}`,
        normalize(command),
      );
    },
  };
}

/** 清理岗位文本字段，并确保新增和更新发送相同形状的请求体。 */
function normalize(command: PositionCommand): PositionCommand {
  // 可选文本统一转为空字符串，保证新增和更新请求体结构一致。
  return {
    positionLevel: command.positionLevel?.trim() || '',
    positionName: command.positionName.trim(),
    remark: command.remark?.trim() || '',
    sort: command.sort,
  };
}
