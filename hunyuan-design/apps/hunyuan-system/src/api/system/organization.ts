import { requestClient } from '#/api/request';

export interface PositionRecord {
  positionId: number;
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort?: null | number;
}

export interface PositionPageQueryParams {
  keywords?: string;
  pageNum: number;
  pageSize: number;
}

export interface PositionAddForm {
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort: number;
}

export interface PositionUpdateForm extends PositionAddForm {
  positionId: number;
}

export async function listPositions() {
  return requestClient.get<PositionRecord[]>('/position/queryList');
}

export function buildPositionMutationPayload<
  T extends PositionAddForm | PositionUpdateForm,
>(params: T): T {
  return {
    ...params,
    positionLevel: params.positionLevel?.trim() || '',
    positionName: params.positionName.trim(),
    remark: params.remark?.trim() || '',
  };
}

export async function queryPositionPage(params: PositionPageQueryParams) {
  return requestClient.post<{
    emptyFlag?: boolean;
    list: PositionRecord[];
    pageNum: number;
    pages: number;
    pageSize: number;
    total: number;
  }>('/position/queryPage', {
    keywords: params.keywords?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  });
}

export async function addPosition(params: PositionAddForm) {
  return requestClient.post<string>(
    '/position/add',
    buildPositionMutationPayload(params),
  );
}

export async function updatePosition(params: PositionUpdateForm) {
  return requestClient.post<string>(
    '/position/update',
    buildPositionMutationPayload(params),
  );
}

export async function deletePosition(positionId: number) {
  return requestClient.get<string>(`/position/delete/${positionId}`);
}

export async function batchDeletePositions(positionIds: number[]) {
  return requestClient.post<string>('/position/batchDelete', positionIds);
}
