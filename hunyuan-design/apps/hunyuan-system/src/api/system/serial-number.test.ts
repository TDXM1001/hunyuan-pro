import { describe, expect, it } from 'vitest';

import {
  buildSerialNumberGeneratePayload,
  buildSerialNumberRecordQueryPayload,
} from './serial-number';

describe('serial number api payloads', () => {
  it('builds record query payloads with paging and serial number id', () => {
    expect(
      buildSerialNumberRecordQueryPayload({
        pageNum: 2,
        pageSize: 20,
        serialNumberId: 5,
      }),
    ).toEqual({
      pageNum: 2,
      pageSize: 20,
      serialNumberId: 5,
    });
  });

  it('builds generate payloads for manual serial number testing', () => {
    expect(
      buildSerialNumberGeneratePayload({
        count: 3,
        serialNumberId: 5,
      }),
    ).toEqual({
      count: 3,
      serialNumberId: 5,
    });
  });
});
