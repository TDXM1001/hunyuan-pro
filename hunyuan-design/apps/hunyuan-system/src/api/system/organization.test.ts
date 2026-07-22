import { describe, expect, it } from 'vitest';

import { buildPositionMutationPayload } from './organization';

describe('组织模块兼容 API 请求体', () => {
  it('岗位请求体会裁剪文本字段', () => {
    expect(
      buildPositionMutationPayload({
        positionLevel: ' L2 ',
        positionName: ' 高级工程师 ',
        remark: ' 平台研发 ',
        sort: 3,
      }),
    ).toEqual({
      positionLevel: 'L2',
      positionName: '高级工程师',
      remark: '平台研发',
      sort: 3,
    });
  });
});
