<script lang="ts" setup>
import { AuthenticationQrCodeLogin } from '@hunyuan/common-ui';
import { LOGIN_PATH } from '@hunyuan/constants';

defineOptions({ name: 'QrCodeLogin' });
</script>

<template>
  <AuthenticationQrCodeLogin :login-path="LOGIN_PATH" />
</template>
