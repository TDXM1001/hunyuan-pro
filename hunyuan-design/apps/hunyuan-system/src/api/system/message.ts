import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface MessageRecord {
  content: string;
  createTime?: null | string;
  dataId?: null | string;
  messageId: number;
  messageType: number;
  readFlag?: boolean;
  readTime?: null | string;
  receiverUserId: number;
  receiverUserType: number;
  title: string;
}

export interface MessagePageQueryParams {
  endDate?: null | string;
  messageType?: null | number;
  pageNum: number;
  pageSize: number;
  readFlag?: boolean;
  receiverUserId?: null | number;
  receiverUserType?: null | number;
  searchWord?: null | string;
  startDate?: null | string;
}

export interface MessageSendFormModel {
  content: string;
  dataId?: null | string;
  messageType: number;
  receiverUserId: number;
  receiverUserType: number;
  title: string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildMessagePageQueryPayload(params: MessagePageQueryParams) {
  return {
    endDate: cleanText(params.endDate) || undefined,
    messageType: params.messageType,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    readFlag: params.readFlag,
    receiverUserId: params.receiverUserId,
    receiverUserType: params.receiverUserType,
    searchWord: cleanText(params.searchWord) || undefined,
    startDate: cleanText(params.startDate) || undefined,
  };
}

export function buildMessageSendPayload(params: MessageSendFormModel) {
  return [
    {
      content: params.content.trim(),
      dataId: cleanText(params.dataId) || undefined,
      messageType: params.messageType,
      receiverUserId: params.receiverUserId,
      receiverUserType: params.receiverUserType,
      title: params.title.trim(),
    },
  ];
}

export async function queryMessagePage(params: MessagePageQueryParams) {
  return requestClient.post<PageResult<MessageRecord>>(
    '/message/query',
    buildMessagePageQueryPayload(params),
  );
}

export async function sendMessage(params: MessageSendFormModel) {
  return requestClient.post<string>(
    '/message/sendMessages',
    buildMessageSendPayload(params),
  );
}

export async function deleteMessage(messageId: number) {
  return requestClient.get<string>(`/message/delete/${messageId}`);
}
