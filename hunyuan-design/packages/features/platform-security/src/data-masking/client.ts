import type { RequestClient } from '@vben/request';

export interface DataMaskingRecord {
  address?: null | string;
  bankCard?: null | string;
  carLicense?: null | string;
  email?: null | string;
  idCard?: null | string;
  other?: null | string;
  password?: null | string;
  phone?: null | string;
  userId?: null | number;
}

export function buildDataMaskingPath() {
  return '/support/dataMasking/demo/query';
}

export async function queryDataMaskingList(requestClient: RequestClient) {
  return requestClient.get<DataMaskingRecord[]>(buildDataMaskingPath());
}
