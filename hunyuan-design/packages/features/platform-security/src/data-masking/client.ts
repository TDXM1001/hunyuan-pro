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
  // 这是验证脱敏效果的演示接口，页面只展示后端已处理的结果，不在浏览器侧重新脱敏。
  return '/support/dataMasking/demo/query';
}

export async function queryDataMaskingList(requestClient: RequestClient) {
  // 请求后端返回已脱敏的示例数据，前端不接触真实敏感值，也不重复实现脱敏规则。
  return requestClient.get<DataMaskingRecord[]>(buildDataMaskingPath());
}
