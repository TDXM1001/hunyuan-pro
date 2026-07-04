<script setup lang="ts">
import type {
  MessagePageQueryParams,
  MessageRecord,
  MessageSendFormModel,
} from '#/api/system/message';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  deleteMessage,
  queryMessagePage,
  sendMessage,
} from '#/api/system/message';

defineOptions({ name: 'SystemSupportMessageList' });

const messageTypeOptions = [
  { label: '站内信', value: 1 },
  { label: '订单', value: 2 },
] as const;

const receiverUserTypeOptions = [{ label: '员工', value: 1 }] as const;

interface MessageFormModel
  extends Omit<MessageSendFormModel, 'receiverUserId'> {
  receiverUserId?: number;
}

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<MessageRecord[]>([]);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive<MessagePageQueryParams>({
  messageType: undefined,
  pageNum: 1,
  pageSize: 10,
  readFlag: undefined,
  searchWord: '',
});

const formData = reactive<MessageFormModel>({
  content: '',
  dataId: '',
  messageType: 1,
  receiverUserId: undefined,
  receiverUserType: 1,
  title: '',
});

const rules: FormRules<MessageFormModel> = {
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
  messageType: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  receiverUserId: [
    { required: true, message: '请输入接收人 ID', trigger: 'change' },
  ],
  receiverUserType: [
    { required: true, message: '请选择接收人类型', trigger: 'change' },
  ],
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<MessageRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'messageType',
    label: '消息类型',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  { prop: 'title', label: '标题', minWidth: 180 },
  {
    prop: 'content',
    label: '内容',
    minWidth: 280,
    formatter: (row) => row.content || '-',
  },
  {
    prop: 'receiverUserType',
    label: '接收人类型',
    width: 120,
    formatter: (row) =>
      receiverUserTypeOptions.find((item) => item.value === row.receiverUserType)
        ?.label || '-',
  },
  { prop: 'receiverUserId', label: '接收人 ID', width: 110, align: 'center' },
  {
    prop: 'readFlag',
    label: '已读状态',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  { prop: 'createTime', label: '创建时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 72,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resetForm() {
  Object.assign(formData, {
    content: '',
    dataId: '',
    messageType: 1,
    receiverUserId: undefined,
    receiverUserType: 1,
    title: '',
  });
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryMessagePage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.size,
    });
    rows.value = result?.list ?? [];
    pagination.total = result?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  Object.assign(searchForm, {
    messageType: undefined,
    readFlag: undefined,
    searchWord: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openSendDialog() {
  resetForm();
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !formData.receiverUserId) {
    return;
  }

  await sendMessage({
    content: formData.content,
    dataId: formData.dataId,
    messageType: formData.messageType,
    receiverUserId: formData.receiverUserId,
    receiverUserType: formData.receiverUserType,
    title: formData.title,
  });
  dialogVisible.value = false;
  ElMessage.success('发送消息成功');
  await loadData();
}

async function handleDelete(row: MessageRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除消息“${row.title}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await deleteMessage(row.messageId);
    ElMessage.success('消息删除成功');
    await loadData();
  } catch {
    // 用户取消
  }
}

function handleCurrentChange(value: number) {
  pagination.current = value;
  void loadData();
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
  void loadData();
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="message-page">
      <ElCard
        v-show="showSearchBar"
        class="message-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="message-page__keyword-item" label="关键字">
            <ElInput
              v-model="searchForm.searchWord"
              clearable
              placeholder="请输入标题或内容"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="message-page__message-type-item" label="消息类型">
            <ElSelect
              v-model="searchForm.messageType"
              clearable
              placeholder="请选择消息类型"
            >
              <ElOption
                v-for="item in messageTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="message-page__read-flag-item" label="已读状态">
            <ElSelect
              v-model="searchForm.readFlag"
              clearable
              placeholder="请选择已读状态"
            >
              <ElOption :value="true" label="已读" />
              <ElOption :value="false" label="未读" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="message-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openSendDialog">发送消息</ElButton>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="columns"
            :data="rows"
            :height="tableHeight"
            :loading="loading"
            :pagination="pagination"
            :pagination-options="{
              align: 'center',
              hideOnSinglePage: false,
              layout: 'sizes, prev, pager, next, jumper',
              pageSizes: [10, 20, 30],
              showTotalSummary: true,
              size: 'small',
            }"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #messageType="{ row }">
              <ElTag effect="plain" size="small" type="primary">
                {{
                  messageTypeOptions.find((item) => item.value === row.messageType)
                    ?.label || '未知'
                }}
              </ElTag>
            </template>

            <template #readFlag="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="row.readFlag ? 'success' : 'warning'"
              >
                {{ row.readFlag ? '已读' : '未读' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="message-page__actions">
                <ElButton
                  link
                  size="small"
                  type="danger"
                  @click="handleDelete(row)"
                >
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      title="发送消息"
      width="640px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <ElFormItem label="消息类型" prop="messageType">
          <ElSelect v-model="formData.messageType" placeholder="请选择消息类型">
            <ElOption
              v-for="item in messageTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="接收人类型" prop="receiverUserType">
          <ElSelect
            v-model="formData.receiverUserType"
            placeholder="请选择接收人类型"
          >
            <ElOption
              v-for="item in receiverUserTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="接收人 ID" prop="receiverUserId">
          <ElInputNumber
            v-model="formData.receiverUserId"
            :min="1"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="消息标题" prop="title">
          <ElInput v-model="formData.title" placeholder="请输入消息标题" />
        </ElFormItem>
        <ElFormItem label="消息内容" prop="content">
          <ElInput
            v-model="formData.content"
            :rows="5"
            placeholder="请输入消息内容"
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="业务 ID" prop="dataId">
          <ElInput v-model="formData.dataId" placeholder="请输入关联业务 ID" />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">发送</ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.message-page__search-card,
.message-page__table-card {
  border-radius: 8px;
}

.message-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.message-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.message-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.message-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.message-page :deep(.art-table-panel),
.message-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.message-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.message-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.message-page__keyword-item :deep(.el-form-item__content) {
  width: 220px;
}

.message-page__message-type-item :deep(.el-form-item__content),
.message-page__read-flag-item :deep(.el-form-item__content) {
  width: 160px;
}

.message-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.message-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.message-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .message-page__keyword-item :deep(.el-form-item__content),
  .message-page__message-type-item :deep(.el-form-item__content),
  .message-page__read-flag-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
