import type { RequestClient } from '@vben/request';

/** 文件管理只处理元数据和访问地址，不把文件二进制内容放进列表状态。 */
export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface FileRecord {
  createTime?: null | string;
  creatorId?: null | number;
  creatorName?: null | string;
  creatorUserType?: null | number;
  fileId: number;
  fileKey: string;
  fileName: string;
  fileSize?: null | number;
  fileType?: null | string;
  fileUrl?: null | string;
  folderType?: null | number;
}

export interface FilePageQueryParams {
  createTimeBegin?: null | string;
  createTimeEnd?: null | string;
  creatorName?: null | string;
  fileKey?: null | string;
  fileName?: null | string;
  fileType?: null | string;
  folderType?: null | number;
  pageNum: number;
  pageSize: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildFilePageQueryPayload(params: FilePageQueryParams) {
  // 将文件名、文件 key、创建人、时间范围和文件类型转换成分页查询参数。
  return {
    createTimeBegin: cleanText(params.createTimeBegin) || undefined,
    createTimeEnd: cleanText(params.createTimeEnd) || undefined,
    creatorName: cleanText(params.creatorName) || undefined,
    fileKey: cleanText(params.fileKey) || undefined,
    fileName: cleanText(params.fileName) || undefined,
    fileType: cleanText(params.fileType) || undefined,
    folderType: params.folderType,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export function buildFilePreviewPath(fileKey: string) {
  // fileKey 可能包含目录分隔符和空格，作为 query 参数前必须编码，避免截断文件定位。
  return `/admin/v1/platform/files/url?fileKey=${encodeURIComponent(fileKey.trim())}`;
}

export function buildFileDownloadPath(fileKey: string) {
  // 下载链接由浏览器直接打开，需要显式走前端 /api 代理。
  const apiPrefix = import.meta.env.VITE_GLOB_API_URL || '/api';
  return `${apiPrefix}/admin/v1/platform/files/download?fileKey=${encodeURIComponent(fileKey.trim())}`;
}

export async function queryFilePage(
  requestClient: RequestClient,
  params: FilePageQueryParams,
) {
  // 获取文件管理列表，页面只展示后端返回的文件元数据。
  return requestClient.post<PageResult<FileRecord>>(
    '/admin/v1/platform/files/query',
    buildFilePageQueryPayload(params),
  );
}

export async function getFileUrl(
  requestClient: RequestClient,
  fileKey: string,
) {
  // 根据文件 key 获取预览地址，真实存储地址由后端决定。
  return requestClient.get<string>(buildFilePreviewPath(fileKey));
}
