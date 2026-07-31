import type { RequestClient } from '@vben/request';

export interface ApiEncryptDemoPayload {
  age: number;
  name: string;
}

export interface ApiEncryptEnvelope {
  encryptData: string;
}

export function buildApiEncryptDemoPayload(params: ApiEncryptDemoPayload) {
  // 演示页只清理姓名并保留年龄，最终加密报文由后端测试接口生成。
  return {
    age: params.age,
    name: params.name.trim(),
  };
}

export function buildApiEncryptEnvelope(encryptData: string): ApiEncryptEnvelope {
  // 页面只构造后端约定的报文外壳，不在 feature 内实现加密算法或持有密钥。
  return { encryptData };
}

export async function testResponseEncryptDemo(requestClient: RequestClient, params: ApiEncryptDemoPayload) {
  // 调用后端响应加密演示接口，页面只负责展示返回的原始结果。
  return requestClient.post<unknown>(
    '/support/apiEncrypt/testResponseEncrypt',
    buildApiEncryptDemoPayload(params),
  );
}
