import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

interface BoundaryExpectation {
  file: string;
  forbidden: string[];
  required: string[];
}

// 聚合检查 A4.3 的仓内消费者，防止稳定接口迁移后又回退到历史兼容路由。
const boundaries: BoundaryExpectation[] = [
  {
    file: 'message.ts',
    forbidden: [
      '/message/query',
      '/message/sendMessages',
      '/message/delete/',
      '/message/queryMyMessage',
      '/message/getUnreadCount',
      '/message/read/',
    ],
    required: [
      '/admin/v1/platform/messages/query',
      '/admin/v1/platform/messages',
      '/admin/v1/platform/message-inbox/query',
      '/admin/v1/platform/message-inbox/unread-count',
      '/admin/v1/platform/message-inbox/',
    ],
  },
  {
    file: 'operate-log.ts',
    forbidden: ['/support/operateLog/'],
    required: [
      '/admin/v1/platform/audit/operation-logs/query',
      '/admin/v1/platform/audit/operation-logs/',
    ],
  },
  {
    file: 'login-log.ts',
    forbidden: ['/support/loginLog/'],
    required: ['/admin/v1/platform/audit/login-logs/query'],
  },
  {
    file: 'sms.ts',
    forbidden: [
      '/support/sms/',
      '/api/admin/v1/platform/notifications/sms/',
    ],
    required: [
      '/admin/v1/platform/notifications/sms/templates/query',
      '/admin/v1/platform/notifications/sms/templates',
      '/admin/v1/platform/notifications/sms/send-logs/query',
    ],
  },
];

describe('A4.3 平台边界', () => {
  it.each(boundaries)('$file 只使用稳定平台路由', ({ file, forbidden, required }) => {
    const source = readFileSync(resolve(__dirname, file), 'utf8');

    required.forEach((route) => expect(source).toContain(route));
    forbidden.forEach((route) => expect(source).not.toContain(route));
  });
});
