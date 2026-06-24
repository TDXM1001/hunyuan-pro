<script lang="ts" setup>
import { h } from 'vue';

import { alert, HunyuanButton } from '@hunyuan/common-ui';

import { Result } from 'antdv-next';

function showAlert() {
  alert('This is an alert message');
}

function showIconAlert() {
  alert({
    content: 'This is an alert message with icon',
    icon: 'success',
  });
}

function showCustomAlert() {
  alert({
    buttonAlign: 'center',
    content: h(Result, {
      status: 'success',
      subTitle: '已成功创建订单。订单ID：2017182818828182881',
      title: '操作成功',
    }),
  });
}
</script>
<template>
  <div class="flex gap-4">
    <HunyuanButton @click="showAlert">Alert</HunyuanButton>
    <HunyuanButton @click="showIconAlert">Alert With Icon</HunyuanButton>
    <HunyuanButton @click="showCustomAlert">Alert With Custom Content</HunyuanButton>
  </div>
</template>
