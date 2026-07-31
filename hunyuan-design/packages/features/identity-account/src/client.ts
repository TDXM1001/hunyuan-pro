import type { RequestClient } from '@vben/request';

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

export interface IdentityAccountClient {
  changePassword(data: EmployeePasswordChange): Promise<void>;
  getCurrentProfile(): Promise<EmployeeAccountProfile>;
  getPasswordPolicy(): Promise<boolean>;
  updateAvatar(avatar: string): Promise<void>;
  updateProfile(data: EmployeeProfileUpdate): Promise<void>;
  uploadAvatar(file: File): Promise<FileUploadResult>;
}

const BASE_PATH = '/admin/v1/identity/account';

// 当前账号接口只允许访问登录主体的资料；请求路径不接受页面传入的用户 ID，权限边界由后端会话保证。
/**
 * 账号 feature 通过应用注入的请求客户端访问当前登录人，避免反向依赖应用请求单例。
 */
export function createIdentityAccountClient(
  requestClient: RequestClient,
): IdentityAccountClient {
  // 账号客户端只操作当前登录人的资料、密码和头像；用户范围由后端会话决定。
  return {
    changePassword(data) {
      return requestClient.post<void>(`${BASE_PATH}/me/password`, data);
    },
    getCurrentProfile() {
      return requestClient.get<EmployeeAccountProfile>(`${BASE_PATH}/me`);
    },
    getPasswordPolicy() {
      return requestClient.get<boolean>(`${BASE_PATH}/me/password-policy`);
    },
    updateAvatar(avatar) {
      return requestClient.put<void>(`${BASE_PATH}/me/avatar`, { avatar });
    },
    updateProfile(data) {
      return requestClient.put<void>(`${BASE_PATH}/me/profile`, data);
    },
    uploadAvatar(file) {
      return requestClient.upload<FileUploadResult>('/admin/v1/platform/files', {
        file,
        folder: 1,
      });
    },
  };
}
