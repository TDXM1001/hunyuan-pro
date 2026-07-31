import type { RequestClient } from '@vben/request';

/** 短信模板和发送日志的数据模型；模板编码是更新、启停操作共用的业务主键。 */
export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface SmsTemplateRecord {
  createTime?: null | string;
  disableFlag?: boolean;
  remark?: null | string;
  templateCode: string;
  templateContent: string;
  templateName: string;
  updateTime?: null | string;
}

export interface SmsSendLogRecord {
  createTime?: null | string;
  failReason?: null | string;
  phone: string;
  provider?: null | string;
  requestId?: null | string;
  sendContent: string;
  sendStatus?: null | number;
  sendTime?: null | string;
  smsSendLogId: number;
  templateCode: string;
}

export interface SmsTemplatePageQueryParams {
  disableFlag?: boolean;
  pageNum: number;
  pageSize: number;
  templateCode?: null | string;
  templateName?: null | string;
}

export interface SmsTemplateAddForm {
  disableFlag?: boolean;
  remark?: null | string;
  templateCode: string;
  templateContent: string;
  templateName: string;
}

export interface SmsTemplateUpdateForm extends SmsTemplateAddForm {}

export interface SmsSendLogPageQueryParams {
  endDate?: null | string;
  pageNum: number;
  pageSize: number;
  phone?: null | string;
  sendStatus?: null | number;
  startDate?: null | string;
  templateCode?: null | string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildSmsTemplateQueryPayload(
  params: SmsTemplatePageQueryParams,
) {
  /** 把模板列表的名称、编码、启用状态和分页信息转换成查询请求。 */
  return {
    disableFlag: params.disableFlag,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    templateCode: cleanText(params.templateCode) || undefined,
    templateName: cleanText(params.templateName) || undefined,
  };
}

export function buildSmsTemplateMutationPayload<
  T extends SmsTemplateAddForm | SmsTemplateUpdateForm,
>(params: T): T {
  // 模板编码既是展示字段，也是更新接口的路径主键；其他字段才属于可修改正文。
  return {
    ...params,
    disableFlag: params.disableFlag ?? false,
    remark: cleanText(params.remark),
    templateCode: params.templateCode.trim(),
    templateContent: params.templateContent.trim(),
    templateName: params.templateName.trim(),
  };
}

// 模板编码是稳定接口的路径主键，这里统一做 trim 和 URL 编码。
export function buildSmsTemplateDisabledPath(
  templateCode: string,
) {
  // 模板编码既是业务标识也是 URL 的一部分，编码后才能安全处理特殊字符。
  return `/admin/v1/platform/notifications/sms/templates/${encodeURIComponent(templateCode.trim())}/disabled`;
}

// 更新接口以路径中的模板编码为准，请求体只携带可修改字段。
export function buildSmsTemplateUpdateRequest(params: SmsTemplateUpdateForm) {
  // 更新请求把模板编码放到路径，把名称、正文等可修改字段放到 body，符合后端主键契约。
  const payload = buildSmsTemplateMutationPayload(params);
  const { templateCode, ...body } = payload;
  return {
    body,
    path: `/admin/v1/platform/notifications/sms/templates/${encodeURIComponent(templateCode)}`,
  };
}

export function buildSmsSendLogQueryPayload(params: SmsSendLogPageQueryParams) {
  // 空筛选项转换为 undefined，避免后端把空字符串误判为实际过滤条件。
  return {
    endDate: cleanText(params.endDate) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    phone: cleanText(params.phone) || undefined,
    sendStatus: params.sendStatus,
    startDate: cleanText(params.startDate) || undefined,
    templateCode: cleanText(params.templateCode) || undefined,
  };
}

export async function querySmsTemplatePage(
  requestClient: RequestClient,
  params: SmsTemplatePageQueryParams,
) {
  // 获取短信模板分页列表，页面的筛选条件先由 build 函数清理。
  return requestClient.post<PageResult<SmsTemplateRecord>>(
    '/admin/v1/platform/notifications/sms/templates/query',
    buildSmsTemplateQueryPayload(params),
  );
}

export async function addSmsTemplate(
  requestClient: RequestClient,
  params: SmsTemplateAddForm,
) {
  // 新增模板使用完整表单，模板编码从此成为后续更新和启停操作的稳定标识。
  return requestClient.post<string>(
    '/admin/v1/platform/notifications/sms/templates',
    buildSmsTemplateMutationPayload(params),
  );
}

export async function updateSmsTemplate(
  requestClient: RequestClient,
  params: SmsTemplateUpdateForm,
) {
  // 更新时复用同一请求构造器，防止新增和编辑对编码、正文的处理规则不一致。
  const request = buildSmsTemplateUpdateRequest(params);
  return requestClient.put<string>(request.path, request.body);
}

export async function updateSmsTemplateDisabled(
  requestClient: RequestClient,
  templateCode: string,
  disableFlag: boolean,
) {
  // 启停模板只提交新的 disableFlag，不重新提交短信正文。
  return requestClient.put<string>(
    buildSmsTemplateDisabledPath(templateCode),
    { disableFlag },
  );
}

export async function querySmsSendLogPage(
  requestClient: RequestClient,
  params: SmsSendLogPageQueryParams,
) {
  // 发送日志是只读记录，查询接口不会修改模板或发送状态。
  return requestClient.post<PageResult<SmsSendLogRecord>>(
    '/admin/v1/platform/notifications/sms/send-logs/query',
    buildSmsSendLogQueryPayload(params),
  );
}
