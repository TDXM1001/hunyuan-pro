<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';

import type { UploadRequestOptions } from 'element-plus';

import { computed, onMounted, ref } from 'vue';

import { ProfileBaseSetting } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { ElAvatar, ElButton, ElMessage, ElUpload } from 'element-plus';

import {
  getCurrentAccountApi,
  updateCurrentAccountAvatarApi,
  updateCurrentAccountProfileApi,
  uploadAccountAvatarFileApi,
} from '#/api';

const profileBaseSettingRef = ref();
const avatarUrl = ref('');
const userStore = useUserStore();

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'actualName',
      component: 'Input',
      label: '姓名',
    },
    {
      fieldName: 'loginName',
      component: 'Input',
      label: '用户名',
      componentProps: {
        disabled: true,
      },
    },
    {
      fieldName: 'phone',
      component: 'Input',
      label: '手机号',
    },
    {
      fieldName: 'email',
      component: 'Input',
      label: '邮箱',
    },
    {
      fieldName: 'remark',
      component: 'Textarea',
      label: '备注',
    },
  ];
});

onMounted(async () => {
  const data = await getCurrentAccountApi();
  avatarUrl.value = data.avatar ?? '';
  profileBaseSettingRef.value.getFormApi().setValues(data);
});

/** 上传头像并在文件服务成功后保存账号头像引用。 */
async function handleAvatarUpload(options: UploadRequestOptions) {
  const uploadedFile = await uploadAccountAvatarFileApi(options.file);
  await updateCurrentAccountAvatarApi(uploadedFile.fileKey);
  avatarUrl.value = uploadedFile.fileUrl ?? avatarUrl.value;
  if (userStore.userInfo && avatarUrl.value) {
    userStore.setUserInfo({
      ...userStore.userInfo,
      avatar: avatarUrl.value,
    });
  }
  ElMessage.success('头像更新成功');
  return uploadedFile;
}

async function handleSubmit(values: Record<string, unknown>) {
  await updateCurrentAccountProfileApi({
    actualName: String(values.actualName ?? ''),
    email: String(values.email ?? ''),
    phone: String(values.phone ?? ''),
    remark: String(values.remark ?? ''),
  });
  ElMessage.success('个人资料保存成功');
}
</script>
<template>
  <div class="mb-6 flex items-center gap-4">
    <ElAvatar :size="72" :src="avatarUrl || undefined">
      {{ userStore.userInfo?.realName?.slice(0, 1) ?? '用' }}
    </ElAvatar>
    <ElUpload
      accept="image/*"
      :http-request="handleAvatarUpload"
      :show-file-list="false"
    >
      <ElButton type="primary">更换头像</ElButton>
    </ElUpload>
  </div>
  <ProfileBaseSetting
    ref="profileBaseSettingRef"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
