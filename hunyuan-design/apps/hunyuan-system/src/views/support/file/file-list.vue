<script setup lang="ts">
import type { FilePageQueryParams, FileRecord } from '#/api/system/file';
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
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElSpace,
} from 'element-plus';

import {
  buildFileDownloadPath,
  getFileUrl,
  queryFilePage,
} from '#/api/system/file';

defineOptions({ name: 'SystemSupportFileList' });

const folderTypeOptions = [
  { label: '通用', value: 1 },
  { label: '公告', value: 2 },
  { label: '帮助中心', value: 3 },
  { label: '意见反馈', value: 4 },
] as const;

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<FileRecord[]>([]);
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive<FilePageQueryParams>({
  creatorName: '',
  fileKey: '',
  fileName: '',
  fileType: '',
  folderType: undefined,
  pageNum: 1,
  pageSize: 10,
});

const columnsFactory = (): ColumnOption<FileRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'fileName', label: '文件名称', minWidth: 220 },
  {
    prop: 'folderType',
    label: '目录类型',
    width: 120,
    formatter: (row) =>
      folderTypeOptions.find((item) => item.value === row.folderType)?.label
      || '-',
  },
  {
    prop: 'fileType',
    label: '文件类型',
    minWidth: 140,
    formatter: (row) => row.fileType || '-',
  },
  {
    prop: 'fileSize',
    label: '大小',
    width: 120,
    formatter: (row) => (row.fileSize ? `${row.fileSize} B` : '-'),
  },
  {
    prop: 'creatorName',
    label: '上传人',
    minWidth: 140,
    formatter: (row) => row.creatorName || '-',
  },
  { prop: 'createTime', label: '上传时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 132,
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

async function loadData() {
  loading.value = true;
  try {
    const result = await queryFilePage({
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
    creatorName: '',
    fileKey: '',
    fileName: '',
    fileType: '',
    folderType: undefined,
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

async function handlePreview(row: FileRecord) {
  if (!row.fileKey) {
    ElMessage.warning('当前文件缺少 fileKey');
    return;
  }

  const fileUrl = await getFileUrl(row.fileKey);
  if (!fileUrl) {
    ElMessage.warning('未获取到文件地址');
    return;
  }
  window.open(fileUrl, '_blank', 'noopener,noreferrer');
}

function handleDownload(row: FileRecord) {
  if (!row.fileKey) {
    ElMessage.warning('当前文件缺少 fileKey');
    return;
  }
  window.open(buildFileDownloadPath(row.fileKey), '_blank', 'noopener,noreferrer');
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
    <div class="file-page">
      <ElCard
        v-show="showSearchBar"
        class="file-page__search-card"
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
          <ElFormItem class="file-page__file-name-item" label="文件名称">
            <ElInput
              v-model="searchForm.fileName"
              clearable
              placeholder="请输入文件名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="file-page__file-key-item" label="文件 Key">
            <ElInput
              v-model="searchForm.fileKey"
              clearable
              placeholder="请输入文件 Key"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="file-page__file-type-item" label="文件类型">
            <ElInput
              v-model="searchForm.fileType"
              clearable
              placeholder="请输入文件类型"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="file-page__folder-type-item" label="目录类型">
            <ElSelect
              v-model="searchForm.folderType"
              clearable
              placeholder="请选择目录类型"
            >
              <ElOption
                v-for="item in folderTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="file-page__creator-name-item" label="上传人">
            <ElInput
              v-model="searchForm.creatorName"
              clearable
              placeholder="请输入上传人"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="file-page__table-card" shadow="never">
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
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #actions="{ row }">
              <ElSpace class="file-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="handlePreview(row)"
                >
                  查看链接
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="handleDownload(row)"
                >
                  下载文件
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.file-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.file-page__search-card,
.file-page__table-card {
  border-radius: 8px;
}

.file-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.file-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.file-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.file-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.file-page :deep(.art-table-panel),
.file-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.file-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.file-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.file-page__file-name-item :deep(.el-form-item__content),
.file-page__file-key-item :deep(.el-form-item__content) {
  width: 220px;
}

.file-page__file-type-item :deep(.el-form-item__content),
.file-page__folder-type-item :deep(.el-form-item__content),
.file-page__creator-name-item :deep(.el-form-item__content) {
  width: 168px;
}

.file-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.file-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.file-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .file-page__file-name-item :deep(.el-form-item__content),
  .file-page__file-key-item :deep(.el-form-item__content),
  .file-page__file-type-item :deep(.el-form-item__content),
  .file-page__folder-type-item :deep(.el-form-item__content),
  .file-page__creator-name-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
