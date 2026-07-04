import { describe, expect, it } from 'vitest';

import {
  buildJobEnabledPayload,
  buildJobLogQueryPayload,
  buildJobMutationPayload,
  buildJobPageQueryPayload,
} from './job';

describe('job api payloads', () => {
  it('trims job page search words and preserves trigger and enabled filters', () => {
    expect(
      buildJobPageQueryPayload({
        enabledFlag: true,
        pageNum: 1,
        pageSize: 10,
        searchWord: '  同步任务  ',
        triggerType: 'CRON',
      }),
    ).toEqual({
      enabledFlag: true,
      pageNum: 1,
      pageSize: 10,
      searchWord: '同步任务',
      triggerType: 'CRON',
    });
  });

  it('trims job mutation payload fields and preserves numeric flags', () => {
    expect(
      buildJobMutationPayload({
        enabledFlag: false,
        jobClass: ' com.demo.jobs.SyncJob ',
        jobId: 7,
        jobName: ' 同步任务 ',
        param: '  {\"force\":true}  ',
        remark: ' 每日凌晨同步 ',
        sort: 3,
        triggerType: 'CRON',
        triggerValue: ' 0 0 1 * * ? ',
      }),
    ).toEqual({
      enabledFlag: false,
      jobClass: 'com.demo.jobs.SyncJob',
      jobId: 7,
      jobName: '同步任务',
      param: '{"force":true}',
      remark: '每日凌晨同步',
      sort: 3,
      triggerType: 'CRON',
      triggerValue: '0 0 1 * * ?',
    });
  });

  it('builds enabled payloads for backend state updates', () => {
    expect(
      buildJobEnabledPayload({
        enabledFlag: true,
        jobId: 7,
      }),
    ).toEqual({
      enabledFlag: true,
      jobId: 7,
    });
  });

  it('trims job log query search words and preserves date filters', () => {
    expect(
      buildJobLogQueryPayload({
        endTime: ' 2026-07-04 ',
        jobId: 7,
        pageNum: 1,
        pageSize: 20,
        searchWord: '  同步  ',
        startTime: ' 2026-07-01 ',
        successFlag: false,
      }),
    ).toEqual({
      endTime: '2026-07-04',
      jobId: 7,
      pageNum: 1,
      pageSize: 20,
      searchWord: '同步',
      startTime: '2026-07-01',
      successFlag: false,
    });
  });
});
