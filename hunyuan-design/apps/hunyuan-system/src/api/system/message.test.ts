import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import {
  buildMessagePageQueryPayload,
  buildMessageSendPayload,
} from './message';

describe('message api payloads', () => {
  it('trims message page query fields and preserves backend filters', () => {
    expect(
      buildMessagePageQueryPayload({
        endDate: ' 2026-07-04 ',
        messageType: 1,
        pageNum: 2,
        pageSize: 20,
        readFlag: false,
        receiverUserId: 9,
        receiverUserType: 1,
        searchWord: '  系统通知  ',
        startDate: ' 2026-07-01 ',
      }),
    ).toEqual({
      endDate: '2026-07-04',
      messageType: 1,
      pageNum: 2,
      pageSize: 20,
      readFlag: false,
      receiverUserId: 9,
      receiverUserType: 1,
      searchWord: '系统通知',
      startDate: '2026-07-01',
    });
  });

  it('wraps a single send form into the backend list payload', () => {
    expect(
      buildMessageSendPayload({
        content: '  请尽快处理审批  ',
        dataId: '  OA-20260704-01  ',
        messageType: 2,
        receiverUserId: 3,
        receiverUserType: 1,
        title: '  审批提醒  ',
      }),
    ).toEqual([
      {
        content: '请尽快处理审批',
        dataId: 'OA-20260704-01',
        messageType: 2,
        receiverUserId: 3,
        receiverUserType: 1,
        title: '审批提醒',
      },
    ]);
  });

  it('exposes stable current-user inbox routes without legacy endpoints', () => {
    const source = readFileSync(
      resolve(process.cwd(), 'apps/hunyuan-system/src/api/system/message.ts'),
      'utf8',
    );

    expect(source).toContain("'/admin/v1/platform/message-inbox/query'");
    expect(source).toContain("'/admin/v1/platform/message-inbox/unread-count'");
    expect(source).toContain('/admin/v1/platform/message-inbox/${messageId}/read');
    expect(source).not.toContain("'/message/queryMyMessage'");
    expect(source).not.toContain("'/message/getUnreadCount'");
    expect(source).not.toContain('/message/read/${messageId}');
  });
});
