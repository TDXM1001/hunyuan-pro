import { requestClient } from '#/api/request';

export interface BpmSampleExpenseCreateParams {
  amount: number;
  applicantEmployeeId: number;
  title: string;
}

export interface BpmSampleExpenseVO {
  amount: number;
  applicantEmployeeId: number;
  approvalStatus: number;
  approvedAt?: null | string;
  callbackEventId?: null | string;
  callbackFailFlag: boolean;
  createTime?: null | string;
  expenseId: number;
  instanceId?: null | number;
  rejectedAt?: null | string;
  title: string;
  updateTime?: null | string;
}

export async function createBpmSampleExpense(
  data: BpmSampleExpenseCreateParams,
) {
  return requestClient.post<number>('/bpm/sample/expense/create', {
    amount: data.amount,
    applicantEmployeeId: data.applicantEmployeeId,
    title: data.title.trim(),
  });
}

export async function startBpmSampleExpense(expenseId: number) {
  return requestClient.post<number>(`/bpm/sample/expense/start/${expenseId}`);
}

export async function getBpmSampleExpenseDetail(expenseId: number) {
  return requestClient.get<BpmSampleExpenseVO>(
    `/bpm/sample/expense/detail/${expenseId}`,
  );
}

export async function markNextBpmSampleExpenseCallbackFailed(
  expenseId: number,
) {
  return requestClient.post<string>(
    `/bpm/sample/expense/markNextCallbackFailed/${expenseId}`,
  );
}
