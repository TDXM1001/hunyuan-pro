import { requestClient } from '#/api/request';

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
  return `/admin/v1/platform/notifications/sms/templates/${encodeURIComponent(templateCode.trim())}/disabled`;
}

// 更新接口以路径中的模板编码为准，请求体只携带可修改字段。
export function buildSmsTemplateUpdateRequest(params: SmsTemplateUpdateForm) {
  const payload = buildSmsTemplateMutationPayload(params);
  const { templateCode, ...body } = payload;
  return {
    body,
    path: `/admin/v1/platform/notifications/sms/templates/${encodeURIComponent(templateCode)}`,
  };
}

export function buildSmsSendLogQueryPayload(params: SmsSendLogPageQueryParams) {
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

export async function querySmsTemplatePage(params: SmsTemplatePageQueryParams) {
  return requestClient.post<PageResult<SmsTemplateRecord>>(
    '/admin/v1/platform/notifications/sms/templates/query',
    buildSmsTemplateQueryPayload(params),
  );
}

export async function addSmsTemplate(params: SmsTemplateAddForm) {
  return requestClient.post<string>(
    '/admin/v1/platform/notifications/sms/templates',
    buildSmsTemplateMutationPayload(params),
  );
}

export async function updateSmsTemplate(params: SmsTemplateUpdateForm) {
  const request = buildSmsTemplateUpdateRequest(params);
  return requestClient.put<string>(request.path, request.body);
}

export async function updateSmsTemplateDisabled(
  templateCode: string,
  disableFlag: boolean,
) {
  return requestClient.put<string>(
    buildSmsTemplateDisabledPath(templateCode),
    { disableFlag },
  );
}

export async function querySmsSendLogPage(params: SmsSendLogPageQueryParams) {
  return requestClient.post<PageResult<SmsSendLogRecord>>(
    '/admin/v1/platform/notifications/sms/send-logs/query',
    buildSmsSendLogQueryPayload(params),
  );
}
