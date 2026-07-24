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

export type MessageInboxPageQueryParams = Omit<
  MessagePageQueryParams,
  'receiverUserId' | 'receiverUserType'
>;

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
    '/admin/v1/platform/messages/query',
    buildMessagePageQueryPayload(params),
  );
}

export async function sendMessage(params: MessageSendFormModel) {
  return requestClient.post<string>(
    '/admin/v1/platform/messages',
    buildMessageSendPayload(params),
  );
}

export async function deleteMessage(messageId: number) {
  return requestClient.delete<string>(
    `/admin/v1/platform/messages/${messageId}`,
  );
}

/**
 * 当前用户消息箱接口不接收接收人参数，用户范围由后端登录态强制限定。
 */
export async function queryCurrentMessageInbox(
  params: MessageInboxPageQueryParams,
) {
  return requestClient.post<PageResult<MessageRecord>>(
    '/admin/v1/platform/message-inbox/query',
    buildMessagePageQueryPayload(params),
  );
}

export async function getCurrentMessageUnreadCount() {
  return requestClient.get<number>(
    '/admin/v1/platform/message-inbox/unread-count',
  );
}

export async function markCurrentMessageRead(messageId: number) {
  return requestClient.put<string>(
    `/admin/v1/platform/message-inbox/${messageId}/read`,
  );
}
