import { requestClient } from '#/api/request';

export interface BpmListenerRecord {
  channels: string[];
  listenerCode: string;
  listenerName: string;
}

export interface BpmListenerChannelOption {
  label: string;
  value: string;
}

export async function queryBpmListenerCatalog() {
  return requestClient.get<BpmListenerRecord[]>('/bpm/listener/query');
}

export async function queryBpmListenerChannelOptions() {
  return requestClient.get<BpmListenerChannelOption[]>(
    '/bpm/listener/channelOptions',
  );
}
