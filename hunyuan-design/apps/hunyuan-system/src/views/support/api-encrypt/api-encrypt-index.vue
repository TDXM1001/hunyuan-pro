<script setup lang="ts">
import type { ApiEncryptDemoPayload } from '#/api/system/api-encrypt';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElTag,
} from 'element-plus';

import {
  buildApiEncryptEnvelope,
  testResponseEncryptDemo,
} from '#/api/system/api-encrypt';

defineOptions({ name: 'SystemNetworkSecurityApiEncryptIndex' });

const loading = ref(false);
const rawResponse = ref('');

const demoForm = reactive<ApiEncryptDemoPayload>({
  age: 18,
  name: 'Alice',
});

const sampleEnvelope = computed(() =>
  JSON.stringify(buildApiEncryptEnvelope('base64-cipher-text'), null, 2),
);

const deferredCapabilities = [
  {
    endpoint: '/support/apiEncrypt/testRequestEncrypt',
    title: '请求加密 Contract',
  },
  {
    endpoint: '/support/apiEncrypt/testDecryptAndEncrypt',
    title: '请求解密 + 返回加密 Contract',
  },
  {
    endpoint: '/support/apiEncrypt/testArray',
    title: '数组报文 Contract',
  },
];

function formatResponsePayload(value: unknown) {
  if (typeof value === 'string') {
    return value;
  }

  return JSON.stringify(value, null, 2);
}

async function handleSubmit() {
  loading.value = true;
  try {
    const result = await testResponseEncryptDemo(demoForm);
    rawResponse.value = formatResponsePayload(result);
    ElMessage.success('返回加密接口调用成功');
  } catch (error: any) {
    ElMessage.error(error?.message || '返回加密接口调用失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="api-encrypt-page">
      <ElCard class="api-encrypt-page__card" shadow="never">
        <template #header>返回加密 Live Demo</template>

        <div class="api-encrypt-page__content">
          <ElForm
            class="api-encrypt-page__form"
            :model="demoForm"
            label-position="top"
          >
            <ElFormItem label="姓名">
              <ElInput v-model="demoForm.name" placeholder="请输入姓名" />
            </ElFormItem>
            <ElFormItem label="年龄">
              <ElInputNumber
                v-model="demoForm.age"
                :min="1"
                placeholder="请输入年龄"
              />
            </ElFormItem>
          </ElForm>

          <div class="api-encrypt-page__actions">
            <ElButton :loading="loading" type="primary" @click="handleSubmit">
              调用 testResponseEncrypt
            </ElButton>
            <ElTag type="warning">页面只展示后端返回的原始加密结果</ElTag>
          </div>

          <div class="api-encrypt-page__result-block">
            <div class="api-encrypt-page__result-label">原始响应</div>
            <pre class="api-encrypt-page__result">{{ rawResponse || '尚未调用接口' }}</pre>
          </div>
        </div>
      </ElCard>

      <ElCard class="api-encrypt-page__card" shadow="never">
        <template #header>请求加密一期边界</template>

        <div class="api-encrypt-page__content">
          <ElTag type="warning">第一阶段不新增前端加密实现</ElTag>
          <p class="api-encrypt-page__hint">
            当前前端只展示后端要求的报文形状，不直接在页面内实现 AES 或 SM4 加密。
          </p>
          <div class="api-encrypt-page__result-block">
            <div class="api-encrypt-page__result-label">示例请求包</div>
            <pre class="api-encrypt-page__result">{{ sampleEnvelope }}</pre>
          </div>
        </div>
      </ElCard>

      <ElCard
        v-for="item in deferredCapabilities"
        :key="item.endpoint"
        class="api-encrypt-page__card"
        shadow="never"
      >
        <template #header>{{ item.title }}</template>

        <div class="api-encrypt-page__content">
          <div class="api-encrypt-page__endpoint">{{ item.endpoint }}</div>
          <pre class="api-encrypt-page__result">{{ sampleEnvelope }}</pre>
        </div>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.api-encrypt-page {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 0;
}

.api-encrypt-page__card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  min-height: 0;
}

.api-encrypt-page__card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  padding: 16px;
}

.api-encrypt-page__content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.api-encrypt-page__form {
  display: grid;
  gap: 12px;
}

.api-encrypt-page__form :deep(.el-input-number) {
  width: 100%;
}

.api-encrypt-page__actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.api-encrypt-page__hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 22px;
  margin: 0;
}

.api-encrypt-page__endpoint,
.api-encrypt-page__result-label {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.api-encrypt-page__result-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.api-encrypt-page__result {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  margin: 0;
  min-height: 120px;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (width <= 1024px) {
  .api-encrypt-page {
    grid-template-columns: 1fr;
  }
}
</style>
