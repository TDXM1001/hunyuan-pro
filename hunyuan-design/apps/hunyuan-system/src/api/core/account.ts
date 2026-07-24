import { requestClient } from '#/api/request';

export interface EmployeeAccountProfile {
  actualName: string;
  avatar?: null | string;
  departmentId?: null | number;
  departmentName?: null | string;
  disabled?: boolean;
  email?: null | string;
  employeeId: number;
  gender?: null | number;
  loginName: string;
  phone?: null | string;
  positionId?: null | number;
}

export interface EmployeeProfileUpdate {
  actualName: string;
  avatar?: null | string;
  email: string;
  gender?: null | number;
  phone: string;
  positionId?: null | number;
  remark?: null | string;
}

export interface EmployeePasswordChange {
  newPassword: string;
  oldPassword: string;
}

export interface FileUploadResult {
  fileKey: string;
  fileUrl?: null | string;
}

const BASE_PATH = '/admin/v1/identity/account';

/** 获取当前登录员工资料。 */
export function getCurrentAccountApi() {
  return requestClient.get<EmployeeAccountProfile>(`${BASE_PATH}/me`);
}

/** 更新当前登录员工允许自助修改的资料。 */
export function updateCurrentAccountProfileApi(data: EmployeeProfileUpdate) {
  return requestClient.put<void>(`${BASE_PATH}/me/profile`, data);
}

/** 上传个人头像文件并返回文件引用。 */
export function uploadAccountAvatarFileApi(file: File) {
  return requestClient.upload<FileUploadResult>('/admin/v1/platform/files', {
    file,
    folder: 1,
  });
}

/** 保存当前登录员工的头像文件引用。 */
export function updateCurrentAccountAvatarApi(avatar: string) {
  return requestClient.put<void>(`${BASE_PATH}/me/avatar`, { avatar });
}

/** 获取当前登录员工的密码策略状态。 */
export function getCurrentAccountPasswordPolicyApi() {
  return requestClient.get<boolean>(
    `${BASE_PATH}/me/password-policy`,
  );
}

/** 修改当前登录员工密码。 */
export function changeCurrentAccountPasswordApi(
  data: EmployeePasswordChange,
) {
  return requestClient.post<void>(`${BASE_PATH}/me/password`, data);
}
