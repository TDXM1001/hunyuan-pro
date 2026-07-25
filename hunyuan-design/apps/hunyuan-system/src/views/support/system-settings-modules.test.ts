import { existsSync, readFileSync } from 'node:fs';

import { describe, expect, it } from 'vitest';

import { resolveWorkspacePath } from '../../test-utils/workspace-path';

const configPagePath =
  'packages/features/platform-configuration/src/configuration/index.vue';
const configApiPath =
  'packages/features/platform-configuration/src/configuration/client.ts';
const dictPagePath =
  'packages/features/platform-configuration/src/dictionary/index.vue';
const dictDrawerPath =
  'packages/features/platform-configuration/src/dictionary/components/dict-data-drawer.vue';
const dictApiPath =
  'packages/features/platform-configuration/src/dictionary/client.ts';
const filePagePath = 'packages/features/platform-file/src/management/index.vue';
const fileApiPath = 'packages/features/platform-file/src/management/client.ts';
const messagePagePath =
  'packages/features/platform-notification/src/message/index.vue';
const messageApiPath = 'packages/features/platform-notification/src/message/client.ts';
const jobPagePath = 'packages/features/platform-runtime/src/job/index.vue';
const jobDrawerPath =
  'packages/features/platform-runtime/src/job/components/job-log-drawer.vue';
const jobApiPath = 'packages/features/platform-runtime/src/job/client.ts';
const serialNumberPagePath =
  'packages/features/platform-runtime/src/serial-number/index.vue';
const serialNumberDrawerPath =
  'packages/features/platform-runtime/src/serial-number/components/serial-number-record-drawer.vue';
const serialNumberApiPath =
  'packages/features/platform-runtime/src/serial-number/client.ts';
const cachePagePath =
  'packages/features/platform-runtime/src/cache/index.vue';
const cacheDrawerPath =
  'packages/features/platform-runtime/src/cache/components/cache-key-drawer.vue';
const cacheApiPath = 'packages/features/platform-runtime/src/cache/client.ts';
const reloadPagePath =
  'packages/features/platform-runtime/src/reload/index.vue';
const reloadDrawerPath =
  'packages/features/platform-runtime/src/reload/components/reload-result-drawer.vue';
const reloadApiPath = 'packages/features/platform-runtime/src/reload/client.ts';
const smsTemplatePagePath =
  'packages/features/platform-notification/src/sms/template-list.vue';
const smsSendLogPagePath =
  'packages/features/platform-notification/src/sms/send-log-list.vue';
const smsApiPath = 'packages/features/platform-notification/src/sms/client.ts';
const smsMenuPatchSqlPath = '../数据库SQL脚本/mysql/sql-update-log/v3.33.0.sql';

describe('system settings support modules', () => {
  it('provides a real parameter config page at the backend-defined component path', () => {
    const pagePath = resolveWorkspacePath(configPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('PlatformConfigurationPage');
  });

  it('keeps the parameter config page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolveWorkspacePath(configPagePath), 'utf8');

    expect(source).not.toContain('config-page__title');
    expect(source).not.toContain('config-page__hero');
    expect(source).not.toContain('config-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires the parameter config api module to the backend config endpoints', () => {
    const apiPath = resolveWorkspacePath(configApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/configurations/query'");
    expect(source).toContain("'/admin/v1/platform/configurations'");
    expect(source).toContain(
      '`/admin/v1/platform/configurations/${params.configId}`',
    );
    expect(source).toContain('buildConfigPageQueryPayload');
    expect(source).toContain('buildConfigMutationPayload');
  });

  it('surfaces the config key, name, value, and remark fields on the page', () => {
    const source = readFileSync(resolveWorkspacePath(configPagePath), 'utf8');

    expect(source).toContain('configKey');
    expect(source).toContain('configName');
    expect(source).toContain('configValue');
    expect(source).toContain('remark');
  });

  it('provides a real dictionary management page at the backend-defined component path', () => {
    const pagePath = resolveWorkspacePath(dictPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('PlatformConfigurationDictionaryPage');
  });

  it('keeps the dictionary page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolveWorkspacePath(dictPagePath), 'utf8');

    expect(source).not.toContain('dict-page__title');
    expect(source).not.toContain('dict-page__hero');
    expect(source).not.toContain('dict-page__desc');
    expect(source).toContain(':collapsible="false"');
    expect(source).toContain('useVbenDrawer');
    expect(source).toContain('DictDataDrawer');
    expect(source).toContain('openDictDataDrawer');
  });

  it('provides a dedicated drawer-based dictionary value surface', () => {
    const drawerPath = resolveWorkspacePath(dictDrawerPath);

    expect(existsSync(drawerPath)).toBe(true);

    const source = readFileSync(drawerPath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('PlatformConfigurationDictDataDrawer');
  });

  it('wires the dictionary api module to the backend dict and dictData endpoints', () => {
    const apiPath = resolveWorkspacePath(dictApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/dictionaries/query'");
    expect(source).toContain("'/admin/v1/platform/dictionaries'");
    expect(source).toContain('`/admin/v1/platform/dictionaries/${dictId}`');
    expect(source).toContain(
      '`/admin/v1/platform/dictionaries/${dictId}/items`',
    );
    expect(source).toContain("'/admin/v1/platform/dictionaries/items'");
    expect(source).toContain(
      '`/admin/v1/platform/dictionaries/${params.dictId}/items/${params.dictDataId}`',
    );
    expect(source).toContain('buildDictPageQueryPayload');
    expect(source).toContain('buildDictMutationPayload');
    expect(source).toContain('buildDictDataMutationPayload');
  });

  it('surfaces dictionary key fields on the page and dictionary-item key fields in the drawer', () => {
    const pageSource = readFileSync(resolveWorkspacePath(dictPagePath), 'utf8');
    const drawerSource = readFileSync(
      resolveWorkspacePath(dictDrawerPath),
      'utf8',
    );

    expect(pageSource).toContain('dictName');
    expect(pageSource).toContain('dictCode');
    expect(pageSource).toContain('openDictDataDrawer');
    expect(drawerSource).toContain('dataLabel');
    expect(drawerSource).toContain('dataValue');
    expect(drawerSource).toContain('dataStyle');
    expect(drawerSource).toContain('disabledFlag');
  });

  it('provides a real file management page and keeps the page dense', () => {
    const pagePath = resolveWorkspacePath(filePagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('PlatformFileManagementPage');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).not.toContain(':collapsible="false"');
    expect(source).not.toContain('file-page__title');
    expect(source).not.toContain('file-page__hero');
    expect(source).not.toContain('file-page__desc');
    expect(source).not.toContain('ElMessage.error(error?.message');
  });

  it('wires the file api module to the backend file endpoints', () => {
    const apiPath = resolveWorkspacePath(fileApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/files/query'");
    expect(source).toContain('/admin/v1/platform/files/url?fileKey=');
    expect(source).toContain('/admin/v1/platform/files/download?fileKey=');
    expect(source).toContain('buildFilePageQueryPayload');
    expect(source).toContain('buildFilePreviewPath');
    expect(source).toContain('buildFileDownloadPath');
  });

  it('surfaces file query and row-action fields on the file page', () => {
    const source = readFileSync(resolveWorkspacePath(filePagePath), 'utf8');

    expect(source).toContain('fileName');
    expect(source).toContain('fileType');
    expect(source).toContain('fileKey');
    expect(source).toContain('creatorName');
    expect(source).toContain('folderType');
    expect(source).toContain('查看链接');
    expect(source).toContain('下载文件');
  });

  it('provides a real message management page and keeps the page dense', () => {
    const pagePath = resolveWorkspacePath(messagePagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('SystemSupportMessageList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain(':collapsible="false"');
    expect(source).not.toContain('message-page__title');
    expect(source).not.toContain('message-page__hero');
    expect(source).not.toContain('message-page__desc');
    expect(source).not.toContain('ElMessage.error(error?.message');
  });

  it('wires the message api module to the backend message endpoints', () => {
    const apiPath = resolveWorkspacePath(messageApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/messages/query'");
    expect(source).toContain("'/admin/v1/platform/messages'");
    expect(source).toContain('/admin/v1/platform/messages/${messageId}');
    expect(source).not.toContain("'/message/query'");
    expect(source).not.toContain("'/message/sendMessages'");
    expect(source).not.toContain('/message/delete/${messageId}');
    expect(source).toContain('buildMessagePageQueryPayload');
    expect(source).toContain('buildMessageSendPayload');
  });

  it('surfaces message query and send fields on the message page', () => {
    const source = readFileSync(resolveWorkspacePath(messagePagePath), 'utf8');

    expect(source).toContain('searchWord');
    expect(source).toContain('messageType');
    expect(source).toContain('receiverUserId');
    expect(source).toContain('receiverUserType');
    expect(source).toContain('title');
    expect(source).toContain('content');
    expect(source).toContain('发送消息');
  });

  it('provides a real job page and dedicated log drawer surface', () => {
    const pagePath = resolveWorkspacePath(jobPagePath);
    const drawerPath = resolveWorkspacePath(jobDrawerPath);

    expect(existsSync(pagePath)).toBe(true);
    expect(existsSync(drawerPath)).toBe(true);

    const pageSource = readFileSync(pagePath, 'utf8');
    const drawerSource = readFileSync(drawerPath, 'utf8');

    expect(pageSource).toContain('SystemSupportJobList');
    expect(pageSource).toContain('ArtSearchPanel');
    expect(pageSource).toContain('ArtTablePanel');
    expect(pageSource).toContain('ArtTableHeader');
    expect(pageSource).toContain('ArtTable');
    expect(pageSource).toContain(':collapsible="false"');
    expect(pageSource).not.toContain('job-page__title');
    expect(pageSource).not.toContain('job-page__hero');
    expect(pageSource).not.toContain('job-page__desc');
    expect(pageSource).not.toContain('ElMessage.error(error?.message');

    expect(drawerSource).toContain('SystemSupportJobLogDrawer');
    expect(drawerSource).toContain('ArtSearchPanel');
    expect(drawerSource).toContain('ArtTable');
    expect(drawerSource).not.toContain('ElMessage.error(error?.message');
  });

  it('wires the job api module to the backend job endpoints', () => {
    const apiPath = resolveWorkspacePath(jobApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/runtime/jobs'");
    expect(source).toContain('`${JOB_BASE_PATH}/query`');
    expect(source).toContain('`${JOB_BASE_PATH}/enabled`');
    expect(source).toContain('`${JOB_BASE_PATH}/execute`');
    expect(source).toContain('`${JOB_BASE_PATH}/logs/query`');
    expect(source).not.toContain("'/support/job/");
    expect(source).not.toContain('`/support/job/');
    expect(source).toContain('buildJobPageQueryPayload');
    expect(source).toContain('buildJobMutationPayload');
    expect(source).toContain('buildJobEnabledPayload');
    expect(source).toContain('buildJobLogQueryPayload');
  });

  it('surfaces job query and operation fields on the job page', () => {
    const source = readFileSync(resolveWorkspacePath(jobPagePath), 'utf8');

    expect(source).toContain('searchWord');
    expect(source).toContain('triggerType');
    expect(source).toContain('enabledFlag');
    expect(source).toContain('jobName');
    expect(source).toContain('jobClass');
    expect(source).toContain('triggerValue');
    expect(source).toContain('新增任务');
    expect(source).toContain('立即执行');
    expect(source).toContain('执行日志');
  });

  it('provides a real serial-number page and dedicated record drawer surface', () => {
    const pagePath = resolveWorkspacePath(serialNumberPagePath);
    const drawerPath = resolveWorkspacePath(serialNumberDrawerPath);

    expect(existsSync(pagePath)).toBe(true);
    expect(existsSync(drawerPath)).toBe(true);

    const pageSource = readFileSync(pagePath, 'utf8');
    const drawerSource = readFileSync(drawerPath, 'utf8');

    expect(pageSource).toContain('SystemSupportSerialNumberList');
    expect(pageSource).toContain('ArtSearchPanel');
    expect(pageSource).toContain('ArtTablePanel');
    expect(pageSource).toContain('ArtTableHeader');
    expect(pageSource).toContain('ArtTable');
    expect(pageSource).toContain(':collapsible="false"');
    expect(pageSource).not.toContain('ElMessage.error(error?.message');
    expect(drawerSource).toContain('SystemSupportSerialNumberRecordDrawer');
    expect(drawerSource).toContain('ArtSearchPanel');
    expect(drawerSource).toContain('ArtTable');
    expect(drawerSource).not.toContain('ElMessage.error(error?.message');
  });

  it('wires the serial-number api module to stable platform runtime endpoints', () => {
    const apiPath = resolveWorkspacePath(serialNumberApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/runtime/serial-numbers'");
    expect(source).toContain(
      "'/admin/v1/platform/runtime/serial-numbers/records/query'",
    );
    expect(source).toContain(
      "'/admin/v1/platform/runtime/serial-numbers/generate'",
    );
    expect(source).not.toContain("'/support/serialNumber/");
    expect(source).toContain('buildSerialNumberRecordQueryPayload');
    expect(source).toContain('buildSerialNumberGeneratePayload');
  });

  it('surfaces serial-number list, record, and generate fields on the page', () => {
    const pageSource = readFileSync(
      resolveWorkspacePath(serialNumberPagePath),
      'utf8',
    );
    const drawerSource = readFileSync(
      resolveWorkspacePath(serialNumberDrawerPath),
      'utf8',
    );

    expect(pageSource).toContain('serialNumberId');
    expect(pageSource).toContain('businessName');
    expect(pageSource).toContain('format');
    expect(pageSource).toContain('ruleType');
    expect(pageSource).toContain('手动生成');
    expect(drawerSource).toContain('recordDate');
    expect(drawerSource).toContain('lastNumber');
    expect(drawerSource).toContain('count');
  });

  it('provides a real cache page and dedicated cache-key drawer surface', () => {
    const pagePath = resolveWorkspacePath(cachePagePath);
    const drawerPath = resolveWorkspacePath(cacheDrawerPath);

    expect(existsSync(pagePath)).toBe(true);
    expect(existsSync(drawerPath)).toBe(true);

    const pageSource = readFileSync(pagePath, 'utf8');
    const drawerSource = readFileSync(drawerPath, 'utf8');

    expect(pageSource).toContain('SystemSupportCacheList');
    expect(pageSource).toContain('ArtSearchPanel');
    expect(pageSource).toContain('ArtTablePanel');
    expect(pageSource).toContain('ArtTableHeader');
    expect(pageSource).toContain('ArtTable');
    expect(pageSource).toContain(':collapsible="false"');
    expect(pageSource).not.toContain('ElMessage.error(error?.message');
    expect(drawerSource).toContain('SystemSupportCacheKeyDrawer');
    expect(drawerSource).toContain('ArtSearchPanel');
    expect(drawerSource).toContain('ArtTable');
    expect(drawerSource).not.toContain('ElMessage.error(error?.message');
  });

  it('wires the cache api module to the backend cache endpoints', () => {
    const apiPath = resolveWorkspacePath(cacheApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/cache/names'");
    expect(source).toContain(
      '/support/cache/keys/${encodeURIComponent(cacheName.trim())}',
    );
    expect(source).toContain(
      '/support/cache/remove/${encodeURIComponent(cacheName.trim())}',
    );
    expect(source).toContain('buildCacheKeysPath');
    expect(source).toContain('buildCacheRemovePath');
  });

  it('surfaces cache name and key fields on the cache page and drawer', () => {
    const pageSource = readFileSync(
      resolveWorkspacePath(cachePagePath),
      'utf8',
    );
    const drawerSource = readFileSync(
      resolveWorkspacePath(cacheDrawerPath),
      'utf8',
    );

    expect(pageSource).toContain('cacheName');
    expect(pageSource).toContain('查看 Keys');
    expect(pageSource).toContain('删除缓存');
    expect(drawerSource).toContain('cacheKey');
  });

  it('provides a real reload page and dedicated result drawer surface', () => {
    const pagePath = resolveWorkspacePath(reloadPagePath);
    const drawerPath = resolveWorkspacePath(reloadDrawerPath);

    expect(existsSync(pagePath)).toBe(true);
    expect(existsSync(drawerPath)).toBe(true);

    const pageSource = readFileSync(pagePath, 'utf8');
    const drawerSource = readFileSync(drawerPath, 'utf8');

    expect(pageSource).toContain('SystemSupportReloadList');
    expect(pageSource).toContain('ArtSearchPanel');
    expect(pageSource).toContain('ArtTablePanel');
    expect(pageSource).toContain('ArtTableHeader');
    expect(pageSource).toContain('ArtTable');
    expect(pageSource).toContain(':collapsible="false"');
    expect(pageSource).not.toContain('ElMessage.error(error?.message');
    expect(drawerSource).toContain('SystemSupportReloadResultDrawer');
    expect(drawerSource).toContain('ArtSearchPanel');
    expect(drawerSource).toContain('ArtTable');
    expect(drawerSource).not.toContain('ElMessage.error(error?.message');
  });

  it('wires the reload api module to the backend reload endpoints', () => {
    const apiPath = resolveWorkspacePath(reloadApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/admin/v1/platform/runtime/reloads'");
    expect(source).toContain(
      '${RELOAD_BASE_PATH}/${encodeURIComponent(tag.trim())}/results',
    );
    expect(source).not.toContain("'/support/reload/");
    expect(source).not.toContain('`/support/reload/');
    expect(source).toContain('buildReloadMutationPayload');
    expect(source).toContain('buildReloadResultPath');
  });

  it('surfaces reload list and result fields on the page and drawer', () => {
    const pageSource = readFileSync(
      resolveWorkspacePath(reloadPagePath),
      'utf8',
    );
    const drawerSource = readFileSync(
      resolveWorkspacePath(reloadDrawerPath),
      'utf8',
    );

    expect(pageSource).toContain('tag');
    expect(pageSource).toContain('identification');
    expect(pageSource).toContain('args');
    expect(pageSource).toContain('更新配置');
    expect(pageSource).toContain('结果历史');
    expect(drawerSource).toContain('result');
    expect(drawerSource).toContain('exception');
  });

  it('wires the sms api module to the stable platform endpoints', () => {
    const apiPath = resolveWorkspacePath(smsApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain(
      "'/admin/v1/platform/notifications/sms/templates/query'",
    );
    expect(source).toContain(
      "'/admin/v1/platform/notifications/sms/templates'",
    );
    expect(source).toContain(
      '/admin/v1/platform/notifications/sms/templates/${encodeURIComponent(',
    );
    expect(source).toContain(
      "'/admin/v1/platform/notifications/sms/send-logs/query'",
    );
    expect(source).not.toContain("'/support/sms/");
    expect(source).not.toContain("'/api/admin/v1/platform/notifications/sms/");
    expect(source).toContain('buildSmsTemplateQueryPayload');
    expect(source).toContain('buildSmsTemplateMutationPayload');
    expect(source).toContain('buildSmsTemplateDisabledPath');
    expect(source).toContain('buildSmsSendLogQueryPayload');
  });

  it('provides an incremental sql patch that flattens sms pages under system settings', () => {
    const sqlPath = resolveWorkspacePath(smsMenuPatchSqlPath);

    expect(existsSync(sqlPath)).toBe(true);

    const source = readFileSync(sqlPath, 'utf8');
    expect(source).toContain('短信菜单目录改为系统设置下的直接页面');
    expect(source).toContain('WHERE `menu_id` IN (306, 307);');
    expect(source).toContain('`parent_id` = 50');
    expect(source).toContain('WHERE `menu_id` = 305;');
    expect(source).toContain('`deleted_flag` = 1');
  });

  it('provides a real sms template page at the backend-defined component path', () => {
    const pagePath = resolveWorkspacePath(smsTemplatePagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('SystemSupportSmsTemplateList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
  });

  it('keeps the sms template page dense and single-row search only', () => {
    const source = readFileSync(
      resolveWorkspacePath(smsTemplatePagePath),
      'utf8',
    );

    expect(source).toContain(':collapsible="false"');
    expect(source).not.toContain('template-page__title');
    expect(source).not.toContain('template-page__hero');
    expect(source).not.toContain('template-page__desc');
  });

  it('surfaces sms template query and mutation fields on the page', () => {
    const source = readFileSync(
      resolveWorkspacePath(smsTemplatePagePath),
      'utf8',
    );

    expect(source).toContain('templateCode');
    expect(source).toContain('templateName');
    expect(source).toContain('templateContent');
    expect(source).toContain('disableFlag');
    expect(source).toContain('remark');
    expect(source).toContain('新增模板');
  });

  it('provides a real sms send-log page at the backend-defined component path', () => {
    const pagePath = resolveWorkspacePath(smsSendLogPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('SystemSupportSmsSendLogList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
  });

  it('keeps the sms send-log page dense and preserves collapsible multi-filter search', () => {
    const source = readFileSync(
      resolveWorkspacePath(smsSendLogPagePath),
      'utf8',
    );

    expect(source).not.toContain(':collapsible="false"');
    expect(source).not.toContain('send-log-page__title');
    expect(source).not.toContain('send-log-page__hero');
    expect(source).not.toContain('send-log-page__desc');
  });

  it('surfaces sms send-log filter and table fields on the page', () => {
    const source = readFileSync(
      resolveWorkspacePath(smsSendLogPagePath),
      'utf8',
    );

    expect(source).toContain('phone');
    expect(source).toContain('templateCode');
    expect(source).toContain('sendStatus');
    expect(source).toContain('startDate');
    expect(source).toContain('endDate');
    expect(source).toContain('provider');
    expect(source).toContain('requestId');
    expect(source).toContain('sendContent');
    expect(source).toContain('failReason');
  });
});
