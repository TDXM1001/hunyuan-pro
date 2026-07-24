import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

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

  it('uses stable platform runtime routes without legacy endpoints', () => {
    const apiSource = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/api/system/serial-number.ts',
      ),
      'utf8',
    );

    expect(apiSource).toContain("'/admin/v1/platform/runtime/serial-numbers'");
    expect(apiSource).toContain(
      "'/admin/v1/platform/runtime/serial-numbers/records/query'",
    );
    expect(apiSource).toContain(
      "'/admin/v1/platform/runtime/serial-numbers/generate'",
    );
    expect(apiSource).not.toContain("'/support/serialNumber/");
  });
});
