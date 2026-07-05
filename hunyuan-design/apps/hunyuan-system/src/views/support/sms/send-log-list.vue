<script setup lang="ts">
import type { SmsSendLogRecord } from '#/api/system/sms';
import type { ColumnOption } from '@vben/art-hooks/table';

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
  ElCard,
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import { querySmsSendLogPage } from '#/api/system/sms';

defineOptions({ name: 'SystemSupportSmsSendLogList' });

const sendStatusOptions = [
  { label: '待发送', value: 0 },
  { label: '发送成功', value: 1 },
  { label: '发送失败', value: 2 },
] as const;

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<SmsSendLogRecord[]>([]);

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  endDate: '',
  phone: '',
  sendStatus: undefined as number | undefined,
  startDate: '',
  templateCode: '',
});

const columnsFactory = (): ColumnOption<SmsSendLogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'provider',
    label: '供应商',
    minWidth: 120,
    formatter: (row) => row.provider || '-',
  },
  {
    prop: 'requestId',
    label: '请求单号',
    minWidth: 180,
    formatter: (row) => row.requestId || '-',
  },
  { prop: 'phone', label: '手机号', minWidth: 140 },
  { prop: 'templateCode', label: '模板编码', minWidth: 180 },
  {
    prop: 'sendContent',
    label: '发送内容',
    minWidth: 320,
    formatter: (row) => row.sendContent || '-',
  },
  {
    prop: 'sendStatus',
    label: '发送状态',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'failReason',
    label: '失败原因',
    minWidth: 220,
    formatter: (row) => row.failReason || '-',
  },
  {
    prop: 'sendTime',
    label: '发送时间',
    minWidth: 180,
    formatter: (row) => row.sendTime || '-',
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 180,
    formatter: (row) => row.createTime || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

// 发送状态先按后端枚举值做本地映射，本轮不额外引入字典配置。
function resolveSendStatusLabel(value?: null | number) {
  return sendStatusOptions.find((item) => item.value === value)?.label || '-';
}

function resolveSendStatusType(value?: null | number) {
  if (value === 1) {
    return 'success';
  }
  if (value === 2) {
    return 'danger';
  }
  return 'warning';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await querySmsSendLogPage({
      endDate: searchForm.endDate,
      pageNum: pagination.current,
      pageSize: pagination.size,
      phone: searchForm.phone,
      sendStatus: searchForm.sendStatus,
      startDate: searchForm.startDate,
      templateCode: searchForm.templateCode,
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
    endDate: '',
    phone: '',
    sendStatus: undefined,
    startDate: '',
    templateCode: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
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
    <div class="send-log-page">
      <ElCard
        v-show="showSearchBar"
        class="send-log-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="send-log-page__phone-item" label="手机号">
            <ElInput
              v-model="searchForm.phone"
              clearable
              placeholder="请输入手机号"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="send-log-page__template-item" label="模板编码">
            <ElInput
              v-model="searchForm.templateCode"
              clearable
              placeholder="请输入模板编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="send-log-page__status-item" label="发送状态">
            <ElSelect
              v-model="searchForm.sendStatus"
              clearable
              placeholder="请选择发送状态"
            >
              <ElOption
                v-for="item in sendStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="send-log-page__date-item" label="开始日期">
            <ElDatePicker
              v-model="searchForm.startDate"
              placeholder="请选择开始日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
          <ElFormItem class="send-log-page__date-item" label="结束日期">
            <ElDatePicker
              v-model="searchForm.endDate"
              placeholder="请选择结束日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="send-log-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          />

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
            row-key="smsSendLogId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #sendStatus="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="resolveSendStatusType(row.sendStatus)"
              >
                {{ resolveSendStatusLabel(row.sendStatus) }}
              </ElTag>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.send-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.send-log-page__search-card,
.send-log-page__table-card {
  border-radius: 8px;
}

.send-log-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.send-log-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.send-log-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.send-log-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.send-log-page :deep(.art-table-panel),
.send-log-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.send-log-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.send-log-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.send-log-page__phone-item :deep(.el-form-item__content),
.send-log-page__template-item :deep(.el-form-item__content) {
  width: 220px;
}

.send-log-page__status-item :deep(.el-form-item__content),
.send-log-page__date-item :deep(.el-form-item__content) {
  width: 168px;
}

@media (width <= 768px) {
  .send-log-page__phone-item :deep(.el-form-item__content),
  .send-log-page__template-item :deep(.el-form-item__content),
  .send-log-page__status-item :deep(.el-form-item__content),
  .send-log-page__date-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
