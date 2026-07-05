import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const configPagePath = 'apps/hunyuan-system/src/views/support/config/config-list.vue';
const configApiPath = 'apps/hunyuan-system/src/api/system/config.ts';
const dictPagePath = 'apps/hunyuan-system/src/views/support/dict/index.vue';
const dictDrawerPath =
  'apps/hunyuan-system/src/views/support/dict/components/dict-data-drawer.vue';
const dictApiPath = 'apps/hunyuan-system/src/api/system/dict.ts';
const filePagePath = 'apps/hunyuan-system/src/views/support/file/file-list.vue';
const fileApiPath = 'apps/hunyuan-system/src/api/system/file.ts';
const messagePagePath =
  'apps/hunyuan-system/src/views/support/message/message-list.vue';
const messageApiPath = 'apps/hunyuan-system/src/api/system/message.ts';
const jobPagePath = 'apps/hunyuan-system/src/views/support/job/job-list.vue';
const jobDrawerPath =
  'apps/hunyuan-system/src/views/support/job/components/job-log-drawer.vue';
const jobApiPath = 'apps/hunyuan-system/src/api/system/job.ts';
const serialNumberPagePath =
  'apps/hunyuan-system/src/views/support/serial-number/serial-number-list.vue';
const serialNumberDrawerPath =
  'apps/hunyuan-system/src/views/support/serial-number/components/serial-number-record-drawer.vue';
const serialNumberApiPath =
  'apps/hunyuan-system/src/api/system/serial-number.ts';
const cachePagePath = 'apps/hunyuan-system/src/views/support/cache/cache-list.vue';
const cacheDrawerPath =
  'apps/hunyuan-system/src/views/support/cache/components/cache-key-drawer.vue';
const cacheApiPath = 'apps/hunyuan-system/src/api/system/cache.ts';
const reloadPagePath =
  'apps/hunyuan-system/src/views/support/reload/reload-list.vue';
const reloadDrawerPath =
  'apps/hunyuan-system/src/views/support/reload/components/reload-result-drawer.vue';
const reloadApiPath = 'apps/hunyuan-system/src/api/system/reload.ts';
const smsTemplatePagePath =
  'apps/hunyuan-system/src/views/support/sms/template-list.vue';
const smsSendLogPagePath =
  'apps/hunyuan-system/src/views/support/sms/send-log-list.vue';
const smsApiPath = 'apps/hunyuan-system/src/api/system/sms.ts';
const smsMenuPatchSqlPath = '../数据库SQL脚本/mysql/sql-update-log/v3.33.0.sql';

describe('system settings support modules', () => {
  it('provides a real parameter config page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), configPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportConfigList');
  });

  it('keeps the parameter config page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), configPagePath), 'utf8');

    expect(source).not.toContain('config-page__title');
    expect(source).not.toContain('config-page__hero');
    expect(source).not.toContain('config-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires the parameter config api module to the backend config endpoints', () => {
    const apiPath = resolve(process.cwd(), configApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/config/query'");
    expect(source).toContain("'/support/config/add'");
    expect(source).toContain("'/support/config/update'");
    expect(source).toContain('buildConfigPageQueryPayload');
    expect(source).toContain('buildConfigMutationPayload');
  });

  it('surfaces the config key, name, value, and remark fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), configPagePath), 'utf8');

    expect(source).toContain('configKey');
    expect(source).toContain('configName');
    expect(source).toContain('configValue');
    expect(source).toContain('remark');
  });

  it('provides a real dictionary management page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), dictPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportDictIndex');
  });

  it('keeps the dictionary page dense without extra hero or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), dictPagePath), 'utf8');

    expect(source).not.toContain('dict-page__title');
    expect(source).not.toContain('dict-page__hero');
    expect(source).not.toContain('dict-page__desc');
    expect(source).toContain(':collapsible="false"');
    expect(source).toContain('useVbenDrawer');
    expect(source).toContain('DictDataDrawer');
    expect(source).toContain('openDictDataDrawer');
  });

  it('provides a dedicated drawer-based dictionary value surface', () => {
    const drawerPath = resolve(process.cwd(), dictDrawerPath);

    expect(existsSync(drawerPath)).toBe(true);

    const source = readFileSync(drawerPath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('SystemSupportDictDataDrawer');
  });

  it('wires the dictionary api module to the backend dict and dictData endpoints', () => {
    const apiPath = resolve(process.cwd(), dictApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/dict/queryPage'");
    expect(source).toContain("'/support/dict/add'");
    expect(source).toContain("'/support/dict/update'");
    expect(source).toContain('/support/dict/dictData/queryDictData/${dictId}');
    expect(source).toContain("'/support/dict/dictData/add'");
    expect(source).toContain("'/support/dict/dictData/update'");
    expect(source).toContain('buildDictPageQueryPayload');
    expect(source).toContain('buildDictMutationPayload');
    expect(source).toContain('buildDictDataMutationPayload');
  });

  it('surfaces dictionary key fields on the page and dictionary-item key fields in the drawer', () => {
    const pageSource = readFileSync(resolve(process.cwd(), dictPagePath), 'utf8');
    const drawerSource = readFileSync(resolve(process.cwd(), dictDrawerPath), 'utf8');

    expect(pageSource).toContain('dictName');
    expect(pageSource).toContain('dictCode');
    expect(pageSource).toContain('openDictDataDrawer');
    expect(drawerSource).toContain('dataLabel');
    expect(drawerSource).toContain('dataValue');
    expect(drawerSource).toContain('dataStyle');
    expect(drawerSource).toContain('disabledFlag');
  });

  it('provides a real file management page and keeps the page dense', () => {
    const pagePath = resolve(process.cwd(), filePagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('SystemSupportFileList');
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
    const apiPath = resolve(process.cwd(), fileApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/file/queryPage'");
    expect(source).toContain('/support/file/getFileUrl?fileKey=');
    expect(source).toContain('/support/file/downLoad?fileKey=');
    expect(source).toContain('buildFilePageQueryPayload');
    expect(source).toContain('buildFilePreviewPath');
    expect(source).toContain('buildFileDownloadPath');
  });

  it('surfaces file query and row-action fields on the file page', () => {
    const source = readFileSync(resolve(process.cwd(), filePagePath), 'utf8');

    expect(source).toContain('fileName');
    expect(source).toContain('fileType');
    expect(source).toContain('fileKey');
    expect(source).toContain('creatorName');
    expect(source).toContain('folderType');
    expect(source).toContain('查看链接');
    expect(source).toContain('下载文件');
  });

  it('provides a real message management page and keeps the page dense', () => {
    const pagePath = resolve(process.cwd(), messagePagePath);

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
    const apiPath = resolve(process.cwd(), messageApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/message/query'");
    expect(source).toContain("'/message/sendMessages'");
    expect(source).toContain('/message/delete/${messageId}');
    expect(source).toContain('buildMessagePageQueryPayload');
    expect(source).toContain('buildMessageSendPayload');
  });

  it('surfaces message query and send fields on the message page', () => {
    const source = readFileSync(resolve(process.cwd(), messagePagePath), 'utf8');

    expect(source).toContain('searchWord');
    expect(source).toContain('messageType');
    expect(source).toContain('receiverUserId');
    expect(source).toContain('receiverUserType');
    expect(source).toContain('title');
    expect(source).toContain('content');
    expect(source).toContain('发送消息');
  });

  it('provides a real job page and dedicated log drawer surface', () => {
    const pagePath = resolve(process.cwd(), jobPagePath);
    const drawerPath = resolve(process.cwd(), jobDrawerPath);

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
    const apiPath = resolve(process.cwd(), jobApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/job/query'");
    expect(source).toContain("'/support/job/add'");
    expect(source).toContain("'/support/job/update'");
    expect(source).toContain("'/support/job/update/enabled'");
    expect(source).toContain("'/support/job/execute'");
    expect(source).toContain('/support/job/delete?jobId=');
    expect(source).toContain("'/support/job/log/query'");
    expect(source).toContain('buildJobPageQueryPayload');
    expect(source).toContain('buildJobMutationPayload');
    expect(source).toContain('buildJobEnabledPayload');
    expect(source).toContain('buildJobLogQueryPayload');
  });

  it('surfaces job query and operation fields on the job page', () => {
    const source = readFileSync(resolve(process.cwd(), jobPagePath), 'utf8');

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
    const pagePath = resolve(process.cwd(), serialNumberPagePath);
    const drawerPath = resolve(process.cwd(), serialNumberDrawerPath);

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

  it('wires the serial-number api module to the backend serial-number endpoints', () => {
    const apiPath = resolve(process.cwd(), serialNumberApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/serialNumber/all'");
    expect(source).toContain("'/support/serialNumber/queryRecord'");
    expect(source).toContain("'/support/serialNumber/generate'");
    expect(source).toContain('buildSerialNumberRecordQueryPayload');
    expect(source).toContain('buildSerialNumberGeneratePayload');
  });

  it('surfaces serial-number list, record, and generate fields on the page', () => {
    const pageSource = readFileSync(
      resolve(process.cwd(), serialNumberPagePath),
      'utf8',
    );
    const drawerSource = readFileSync(
      resolve(process.cwd(), serialNumberDrawerPath),
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
    const pagePath = resolve(process.cwd(), cachePagePath);
    const drawerPath = resolve(process.cwd(), cacheDrawerPath);

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
    const apiPath = resolve(process.cwd(), cacheApiPath);

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
    const pageSource = readFileSync(resolve(process.cwd(), cachePagePath), 'utf8');
    const drawerSource = readFileSync(
      resolve(process.cwd(), cacheDrawerPath),
      'utf8',
    );

    expect(pageSource).toContain('cacheName');
    expect(pageSource).toContain('查看 Keys');
    expect(pageSource).toContain('删除缓存');
    expect(drawerSource).toContain('cacheKey');
  });

  it('provides a real reload page and dedicated result drawer surface', () => {
    const pagePath = resolve(process.cwd(), reloadPagePath);
    const drawerPath = resolve(process.cwd(), reloadDrawerPath);

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
    const apiPath = resolve(process.cwd(), reloadApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/reload/query'");
    expect(source).toContain("'/support/reload/update'");
    expect(source).toContain(
      '/support/reload/result/${encodeURIComponent(tag.trim())}',
    );
    expect(source).toContain('buildReloadMutationPayload');
    expect(source).toContain('buildReloadResultPath');
  });

  it('surfaces reload list and result fields on the page and drawer', () => {
    const pageSource = readFileSync(resolve(process.cwd(), reloadPagePath), 'utf8');
    const drawerSource = readFileSync(
      resolve(process.cwd(), reloadDrawerPath),
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

  it('wires the sms api module to the backend sms endpoints', () => {
    const apiPath = resolve(process.cwd(), smsApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/support/sms/template/query'");
    expect(source).toContain("'/support/sms/template/add'");
    expect(source).toContain("'/support/sms/template/update'");
    expect(source).toContain(
      '/support/sms/template/updateDisabled/${encodeURIComponent(',
    );
    expect(source).toContain("'/support/sms/sendLog/query'");
    expect(source).toContain('buildSmsTemplateQueryPayload');
    expect(source).toContain('buildSmsTemplateMutationPayload');
    expect(source).toContain('buildSmsTemplateDisabledPath');
    expect(source).toContain('buildSmsSendLogQueryPayload');
  });

  it('provides an incremental sql patch that flattens sms pages under system settings', () => {
    const sqlPath = resolve(process.cwd(), smsMenuPatchSqlPath);

    expect(existsSync(sqlPath)).toBe(true);

    const source = readFileSync(sqlPath, 'utf8');
    expect(source).toContain('短信菜单目录改为系统设置下的直接页面');
    expect(source).toContain('WHERE `menu_id` IN (306, 307);');
    expect(source).toContain('`parent_id` = 50');
    expect(source).toContain('WHERE `menu_id` = 305;');
    expect(source).toContain('`deleted_flag` = 1');
  });

  it('provides a real sms template page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), smsTemplatePagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('SystemSupportSmsTemplateList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
  });

  it('keeps the sms template page dense and single-row search only', () => {
    const source = readFileSync(resolve(process.cwd(), smsTemplatePagePath), 'utf8');

    expect(source).toContain(':collapsible="false"');
    expect(source).not.toContain('template-page__title');
    expect(source).not.toContain('template-page__hero');
    expect(source).not.toContain('template-page__desc');
  });

  it('surfaces sms template query and mutation fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), smsTemplatePagePath), 'utf8');

    expect(source).toContain('templateCode');
    expect(source).toContain('templateName');
    expect(source).toContain('templateContent');
    expect(source).toContain('disableFlag');
    expect(source).toContain('remark');
    expect(source).toContain('新增模板');
  });

  it('provides a real sms send-log page at the backend-defined component path', () => {
    const pagePath = resolve(process.cwd(), smsSendLogPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('SystemSupportSmsSendLogList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
  });

  it('keeps the sms send-log page dense and preserves collapsible multi-filter search', () => {
    const source = readFileSync(resolve(process.cwd(), smsSendLogPagePath), 'utf8');

    expect(source).not.toContain(':collapsible="false"');
    expect(source).not.toContain('send-log-page__title');
    expect(source).not.toContain('send-log-page__hero');
    expect(source).not.toContain('send-log-page__desc');
  });

  it('surfaces sms send-log filter and table fields on the page', () => {
    const source = readFileSync(resolve(process.cwd(), smsSendLogPagePath), 'utf8');

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
