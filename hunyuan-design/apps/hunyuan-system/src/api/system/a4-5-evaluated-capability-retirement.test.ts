import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('A4.5 评估能力退役边界', () => {
  it('管理端不再恢复帮助、反馈和数据追踪消费者', () => {
    const retiredEntries = [
      'src/api/system/help-doc.ts',
      'src/api/system/feedback.ts',
      'src/api/system/data-tracer.ts',
      'src/views/support/help-doc',
      'src/views/support/feedback',
      'src/views/support/data-tracer',
    ];

    retiredEntries.forEach((entry) => {
      expect(existsSync(resolve(__dirname, '../../..', entry))).toBe(false);
    });
  });

  it('文件查询不再暴露已退役的目录类型', () => {
    const fileList = readFileSync(
      resolve(__dirname, '../../views/support/file/file-list.vue'),
      'utf8',
    );

    expect(fileList).not.toContain("label: '帮助中心'");
    expect(fileList).not.toContain("label: '意见反馈'");
    expect(fileList).not.toContain('value: 3');
    expect(fileList).not.toContain('value: 4');
  });
});
