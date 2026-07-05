import { requestClient } from '#/api/request';

export interface ApiEncryptDemoPayload {
  age: number;
  name: string;
}

export interface ApiEncryptEnvelope {
  encryptData: string;
}

export function buildApiEncryptDemoPayload(params: ApiEncryptDemoPayload) {
  return {
    age: params.age,
    name: params.name.trim(),
  };
}

export function buildApiEncryptEnvelope(encryptData: string): ApiEncryptEnvelope {
  return { encryptData };
}

export async function testResponseEncryptDemo(params: ApiEncryptDemoPayload) {
  return requestClient.post<unknown>(
    '/support/apiEncrypt/testResponseEncrypt',
    buildApiEncryptDemoPayload(params),
  );
}
