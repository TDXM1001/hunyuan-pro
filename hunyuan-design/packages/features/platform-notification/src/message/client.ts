import type { RequestClient } from '@vben/request';

/** 消息中心的数据和请求模型，区分后台发送管理与当前登录人的收件箱。 */
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
  // 管理端消息查询支持接收人筛选；收件箱调用同一个转换器时会省略接收人字段。
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
  // 后端发送接口接收消息数组，即使页面一次发送一条，也在客户端固定成批量契约。
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

export async function queryMessagePage(
  requestClient: RequestClient,
  params: MessagePageQueryParams,
) {
  // 查询后台消息管理列表，返回带分页信息的消息记录。
  return requestClient.post<PageResult<MessageRecord>>(
    '/admin/v1/platform/messages/query',
    buildMessagePageQueryPayload(params),
  );
}

export async function sendMessage(
  requestClient: RequestClient,
  params: MessageSendFormModel,
) {
  // 发送消息时客户端固定传数组格式，页面一次发送一条也遵守批量接口契约。
  return requestClient.post<string>(
    '/admin/v1/platform/messages',
    buildMessageSendPayload(params),
  );
}

export async function deleteMessage(requestClient: RequestClient, messageId: number) {
  // 删除操作只按消息主键执行，页面确认提示负责防止误删。
  return requestClient.delete<string>(
    `/admin/v1/platform/messages/${messageId}`,
  );
}

/**
 * 当前用户消息箱接口不接收接收人参数，用户范围由后端登录态强制限定。
 */
export async function queryCurrentMessageInbox(
  requestClient: RequestClient,
  params: MessageInboxPageQueryParams,
) {
  // 当前用户收件箱的用户范围由登录态决定，前端不能通过参数读取其他人的消息。
  return requestClient.post<PageResult<MessageRecord>>(
    '/admin/v1/platform/message-inbox/query',
    buildMessagePageQueryPayload(params),
  );
}

export async function getCurrentMessageUnreadCount(requestClient: RequestClient) {
  // 获取当前登录人的未读数量，用于消息入口的红点或数字提示。
  return requestClient.get<number>(
    '/admin/v1/platform/message-inbox/unread-count',
  );
}

export async function markCurrentMessageRead(
  requestClient: RequestClient,
  messageId: number,
) {
  // 将当前用户的一条消息标记为已读，主键必须来自收件箱列表。
  return requestClient.put<string>(
    `/admin/v1/platform/message-inbox/${messageId}/read`,
  );
}
