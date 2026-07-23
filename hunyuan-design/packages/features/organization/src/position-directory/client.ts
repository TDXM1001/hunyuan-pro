import type { RequestClient } from '@vben/request';

import type {
  OrganizationPositionClient,
  PositionCommand,
  PositionRecord,
} from './contract';

const BASE_PATH = '/admin/v1/organization/positions';

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

function normalize(command: PositionCommand): PositionCommand {
  return {
    positionLevel: command.positionLevel?.trim() || '',
    positionName: command.positionName.trim(),
    remark: command.remark?.trim() || '',
    sort: command.sort,
  };
}
