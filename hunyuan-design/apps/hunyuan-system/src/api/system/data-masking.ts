import { requestClient } from '#/api/request';

export interface DataMaskingDemoRecord {
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

export function buildDataMaskingDemoPath() {
  return '/support/dataMasking/demo/query';
}

export async function queryDataMaskingDemoList() {
  return requestClient.get<DataMaskingDemoRecord[]>(buildDataMaskingDemoPath());
}
