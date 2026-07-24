import { describe, expect, it } from 'vitest';

import {
  buildFileDownloadPath,
  buildFilePageQueryPayload,
  buildFilePreviewPath,
} from './file';

describe('file api payloads', () => {
  it('trims file page query fields and preserves paging values', () => {
    expect(
      buildFilePageQueryPayload({
        createTimeBegin: ' 2026-07-01 ',
        createTimeEnd: ' 2026-07-04 ',
        creatorName: ' 张三 ',
        fileKey: ' private/common/a.png ',
        fileName: ' 资产图纸 ',
        fileType: ' image/png ',
        folderType: 1,
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      createTimeBegin: '2026-07-01',
      createTimeEnd: '2026-07-04',
      creatorName: '张三',
      fileKey: 'private/common/a.png',
      fileName: '资产图纸',
      fileType: 'image/png',
      folderType: 1,
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('builds encoded preview and download paths', () => {
    expect(buildFilePreviewPath(' private/common/demo 1.png ')).toBe(
      '/admin/v1/platform/files/url?fileKey=private%2Fcommon%2Fdemo%201.png',
    );
    expect(buildFileDownloadPath(' private/common/demo 1.png ')).toBe(
      '/api/admin/v1/platform/files/download?fileKey=private%2Fcommon%2Fdemo%201.png',
    );
  });
});
