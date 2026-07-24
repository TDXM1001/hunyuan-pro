import { requestClient } from '#/api/request';

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
  return `/admin/v1/platform/files/url?fileKey=${encodeURIComponent(fileKey.trim())}`;
}

export function buildFileDownloadPath(fileKey: string) {
  // 下载链接由浏览器直接打开，需要显式走前端 /api 代理。
  const apiPrefix = import.meta.env.VITE_GLOB_API_URL || '/api';
  return `${apiPrefix}/admin/v1/platform/files/download?fileKey=${encodeURIComponent(fileKey.trim())}`;
}

export async function queryFilePage(params: FilePageQueryParams) {
  return requestClient.post<PageResult<FileRecord>>(
    '/admin/v1/platform/files/query',
    buildFilePageQueryPayload(params),
  );
}

export async function getFileUrl(fileKey: string) {
  return requestClient.get<string>(buildFilePreviewPath(fileKey));
}
