import { describe, expect, it } from 'vitest';

import {
  buildSmsSendLogQueryPayload,
  buildSmsTemplateDisabledPath,
  buildSmsTemplateMutationPayload,
  buildSmsTemplateQueryPayload,
  buildSmsTemplateUpdateRequest,
} from './sms';

describe('sms api payloads', () => {
  it('trims sms template query fields and preserves disableFlag filters', () => {
    expect(
      buildSmsTemplateQueryPayload({
        disableFlag: false,
        pageNum: 2,
        pageSize: 20,
        templateCode: '  login_code  ',
        templateName: '  登录验证码  ',
      }),
    ).toEqual({
      disableFlag: false,
      pageNum: 2,
      pageSize: 20,
      templateCode: 'login_code',
      templateName: '登录验证码',
    });
  });

  it('trims sms template mutation fields and preserves disableFlag on update', () => {
    expect(
      buildSmsTemplateMutationPayload({
        disableFlag: true,
        remark: '  登录场景模板  ',
        templateCode: '  login_code  ',
        templateContent: '  您的验证码是 ${code}  ',
        templateName: '  登录验证码  ',
      }),
    ).toEqual({
      disableFlag: true,
      remark: '登录场景模板',
      templateCode: 'login_code',
      templateContent: '您的验证码是 ${code}',
      templateName: '登录验证码',
    });
  });

  it('builds the sms template disabled path with encoded templateCode', () => {
    expect(
      buildSmsTemplateDisabledPath('  login code/test  '),
    ).toBe(
      '/admin/v1/platform/notifications/sms/templates/login%20code%2Ftest/disabled',
    );
  });

  it('builds the stable sms template update request', () => {
    expect(
      buildSmsTemplateUpdateRequest({
        disableFlag: false,
        remark: '  登录模板  ',
        templateCode: ' login code/test ',
        templateContent: ' 验证码 ${code} ',
        templateName: ' 登录验证码 ',
      }),
    ).toEqual({
      body: {
        disableFlag: false,
        remark: '登录模板',
        templateContent: '验证码 ${code}',
        templateName: '登录验证码',
      },
      path: '/admin/v1/platform/notifications/sms/templates/login%20code%2Ftest',
    });
  });

  it('trims sms send-log query fields and preserves status and date filters', () => {
    expect(
      buildSmsSendLogQueryPayload({
        endDate: ' 2026-07-05 ',
        pageNum: 1,
        pageSize: 10,
        phone: ' 13800138000 ',
        sendStatus: 2,
        startDate: ' 2026-07-01 ',
        templateCode: ' login_code ',
      }),
    ).toEqual({
      endDate: '2026-07-05',
      pageNum: 1,
      pageSize: 10,
      phone: '13800138000',
      sendStatus: 2,
      startDate: '2026-07-01',
      templateCode: 'login_code',
    });
  });
});
