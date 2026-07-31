<script setup lang="ts">
import { computed } from 'vue';

import { ProfileNotificationSetting } from '@vben/common-ui';

// 通知项由账号 feature 统一定义，后续接入偏好接口时无需改动应用入口。
const formSchema = computed(() => {
  // 当前先展示通知偏好表单结构，后续接入接口时只需在此处绑定真实模型。
  return [
    {
      value: true,
      fieldName: 'accountPassword',
      label: '账户密码',
      description: '其他用户的消息将以站内信的形式通知',
    },
    {
      value: true,
      fieldName: 'systemMessage',
      label: '系统消息',
      description: '系统消息将以站内信的形式通知',
    },
    {
      value: true,
      fieldName: 'todoTask',
      label: '待办任务',
      description: '待办任务将以站内信的形式通知',
    },
  ];
});
</script>
<template>
  <ProfileNotificationSetting :form-schema="formSchema" />
</template>
