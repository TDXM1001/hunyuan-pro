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

// 模板编码是后端主键路径参数，这里统一做 trim + URL 编码，并补齐支撑模块前缀。
export function buildSmsTemplateDisabledPath(
  templateCode: string,
  disableFlag: boolean,
) {
  return `/support/sms/template/updateDisabled/${encodeURIComponent(templateCode.trim())}/${disableFlag}`;
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
    '/support/sms/template/query',
    buildSmsTemplateQueryPayload(params),
  );
}

export async function addSmsTemplate(params: SmsTemplateAddForm) {
  return requestClient.post<string>(
    '/support/sms/template/add',
    buildSmsTemplateMutationPayload(params),
  );
}

export async function updateSmsTemplate(params: SmsTemplateUpdateForm) {
  return requestClient.post<string>(
    '/support/sms/template/update',
    buildSmsTemplateMutationPayload(params),
  );
}

export async function updateSmsTemplateDisabled(
  templateCode: string,
  disableFlag: boolean,
) {
  return requestClient.get<string>(
    buildSmsTemplateDisabledPath(templateCode, disableFlag),
  );
}

export async function querySmsSendLogPage(params: SmsSendLogPageQueryParams) {
  return requestClient.post<PageResult<SmsSendLogRecord>>(
    '/support/sms/sendLog/query',
    buildSmsSendLogQueryPayload(params),
  );
}
