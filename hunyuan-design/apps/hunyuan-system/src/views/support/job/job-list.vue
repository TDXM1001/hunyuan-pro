<script setup lang="ts">
import type {
  JobMutationFormModel,
  JobPageQueryParams,
  JobRecord,
} from '#/api/system/job';
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
import { Page, useVbenDrawer } from '@vben/common-ui';

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
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addJob,
  deleteJob,
  executeJob,
  queryJobPage,
  updateJob,
  updateJobEnabled,
} from '#/api/system/job';

import JobLogDrawerPanel from './components/job-log-drawer.vue';

defineOptions({ name: 'SystemSupportJobList' });

const triggerTypeOptions = [
  { label: 'Cron 表达式', value: 'cron' },
  { label: '固定间隔', value: 'fixed_delay' },
] as const;

interface JobFormModel extends JobMutationFormModel {
  jobId?: number;
}

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<JobRecord[]>([]);
const selectedJob = ref<JobRecord>();
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();

const [JobLogDrawer, jobLogDrawerApi] = useVbenDrawer({
  connectedComponent: JobLogDrawerPanel,
  destroyOnClose: false,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive<JobPageQueryParams>({
  enabledFlag: undefined,
  pageNum: 1,
  pageSize: 10,
  searchWord: '',
  triggerType: '',
});

const formData = reactive<JobFormModel>({
  enabledFlag: true,
  jobClass: '',
  jobName: '',
  param: '',
  remark: '',
  sort: 100,
  triggerType: 'cron',
  triggerValue: '',
});

const rules: FormRules<JobFormModel> = {
  enabledFlag: [{ required: true, message: '请选择启用状态', trigger: 'change' }],
  jobClass: [{ required: true, message: '请输入执行类', trigger: 'blur' }],
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
  triggerType: [{ required: true, message: '请选择触发类型', trigger: 'change' }],
  triggerValue: [{ required: true, message: '请输入触发配置', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<JobRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'jobName', label: '任务名称', minWidth: 180 },
  { prop: 'jobClass', label: '执行类', minWidth: 240 },
  {
    prop: 'triggerType',
    label: '触发类型',
    width: 120,
    align: 'center',
    useSlot: true,
  },
  { prop: 'triggerValue', label: '触发配置', minWidth: 180 },
  {
    prop: 'enabledFlag',
    label: '状态',
    width: 90,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'lastExecuteTime',
    label: '最近执行时间',
    minWidth: 180,
    formatter: (row) => row.lastExecuteTime || '-',
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 180,
    formatter: (row) => row.remark || '-',
  },
  {
    prop: 'actions',
    label: '操作',
    width: 280,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const dialogTitle = computed(() =>
  dialogMode.value === 'add' ? '新增任务' : '编辑任务',
);
const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resolveTriggerTypeLabel(value?: null | string) {
  return (
    triggerTypeOptions.find((item) => item.value === value)?.label
    || value
    || '-'
  );
}

function resetForm() {
  Object.assign(formData, {
    enabledFlag: true,
    jobClass: '',
    jobName: '',
    param: '',
    remark: '',
    sort: 100,
    triggerType: 'cron',
    triggerValue: '',
  });
  formData.jobId = undefined;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryJobPage({
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
    enabledFlag: undefined,
    searchWord: '',
    triggerType: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openAddDialog() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(row: JobRecord) {
  dialogMode.value = 'edit';
  Object.assign(formData, {
    enabledFlag: row.enabledFlag,
    jobClass: row.jobClass,
    jobId: row.jobId,
    jobName: row.jobName,
    param: row.param || '',
    remark: row.remark || '',
    sort: row.sort,
    triggerType: row.triggerType,
    triggerValue: row.triggerValue,
  });
  dialogVisible.value = true;
}

function openJobLogDrawer(row: JobRecord) {
  selectedJob.value = row;
  jobLogDrawerApi.open();
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addJob(formData);
    ElMessage.success('新增任务成功');
  } else {
    await updateJob(formData);
    ElMessage.success('更新任务成功');
  }

  dialogVisible.value = false;
  await loadData();
}

async function handleToggleEnabled(row: JobRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.enabledFlag ? '停用' : '启用'}任务“${row.jobName}”吗？`,
      '状态确认',
      { type: 'warning' },
    );
    await updateJobEnabled({
      enabledFlag: !row.enabledFlag,
      jobId: row.jobId,
    });
    ElMessage.success('任务状态已更新');
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleExecute(row: JobRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要立即执行任务“${row.jobName}”吗？`,
      '执行确认',
      { type: 'warning' },
    );
    await executeJob({
      jobId: row.jobId,
      param: row.param,
    });
    ElMessage.success('任务已提交执行');
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleDelete(row: JobRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务“${row.jobName}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await deleteJob(row.jobId);
    ElMessage.success('任务删除成功');
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
    <div class="job-page">
      <ElCard
        v-show="showSearchBar"
        class="job-page__search-card"
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
          <ElFormItem class="job-page__keyword-item" label="关键字">
            <ElInput
              v-model="searchForm.searchWord"
              clearable
              placeholder="请输入任务名称或执行类"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="job-page__trigger-type-item" label="触发类型">
            <ElSelect
              v-model="searchForm.triggerType"
              clearable
              placeholder="请选择触发类型"
            >
              <ElOption
                v-for="item in triggerTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="job-page__enabled-item" label="状态">
            <ElSelect
              v-model="searchForm.enabledFlag"
              clearable
              placeholder="请选择状态"
            >
              <ElOption :value="true" label="启用" />
              <ElOption :value="false" label="停用" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="job-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">新增任务</ElButton>
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
            row-key="jobId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #triggerType="{ row }">
              <ElTag effect="plain" size="small" type="primary">
                {{ resolveTriggerTypeLabel(row.triggerType) }}
              </ElTag>
            </template>

            <template #enabledFlag="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="row.enabledFlag ? 'success' : 'warning'"
              >
                {{ row.enabledFlag ? '启用' : '停用' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="job-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openEditDialog(row)"
                >
                  编辑
                </ElButton>
                <ElButton
                  link
                  size="small"
                  :type="row.enabledFlag ? 'warning' : 'success'"
                  @click="handleToggleEnabled(row)"
                >
                  {{ row.enabledFlag ? '停用' : '启用' }}
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="handleExecute(row)"
                >
                  立即执行
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openJobLogDrawer(row)"
                >
                  执行日志
                </ElButton>
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

      <JobLogDrawer :job="selectedJob" />

      <ElDialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="680px"
        @closed="resetForm"
      >
        <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
          <ElFormItem label="任务名称" prop="jobName">
            <ElInput v-model="formData.jobName" placeholder="请输入任务名称" />
          </ElFormItem>
          <ElFormItem label="执行类" prop="jobClass">
            <ElInput
              v-model="formData.jobClass"
              placeholder="请输入任务执行类全名"
            />
          </ElFormItem>
          <ElFormItem label="触发类型" prop="triggerType">
            <ElSelect v-model="formData.triggerType" placeholder="请选择触发类型">
              <ElOption
                v-for="item in triggerTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="触发配置" prop="triggerValue">
            <ElInput
              v-model="formData.triggerValue"
              placeholder="请输入 Cron 表达式或固定间隔"
            />
          </ElFormItem>
          <ElFormItem label="排序" prop="sort">
            <ElInputNumber
              v-model="formData.sort"
              :min="0"
              style="width: 100%"
            />
          </ElFormItem>
          <ElFormItem label="是否启用" prop="enabledFlag">
            <ElSwitch
              v-model="formData.enabledFlag"
              inline-prompt
              active-text="启用"
              inactive-text="停用"
            />
          </ElFormItem>
          <ElFormItem label="执行参数" prop="param">
            <ElInput
              v-model="formData.param"
              :rows="4"
              placeholder="请输入执行参数"
              type="textarea"
            />
          </ElFormItem>
          <ElFormItem label="备注" prop="remark">
            <ElInput
              v-model="formData.remark"
              :rows="4"
              maxlength="250"
              placeholder="请输入备注"
              type="textarea"
            />
          </ElFormItem>
        </ElForm>

        <template #footer>
          <ElSpace>
            <ElButton @click="dialogVisible = false">取消</ElButton>
            <ElButton type="primary" @click="handleSubmit">保存</ElButton>
          </ElSpace>
        </template>
      </ElDialog>
    </div>
  </Page>
</template>

<style scoped>
.job-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.job-page__search-card,
.job-page__table-card {
  border-radius: 8px;
}

.job-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.job-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.job-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.job-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.job-page :deep(.art-table-panel),
.job-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.job-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.job-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.job-page__keyword-item :deep(.el-form-item__content) {
  width: 220px;
}

.job-page__trigger-type-item :deep(.el-form-item__content),
.job-page__enabled-item :deep(.el-form-item__content) {
  width: 168px;
}

.job-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.job-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.job-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .job-page__keyword-item :deep(.el-form-item__content),
  .job-page__trigger-type-item :deep(.el-form-item__content),
  .job-page__enabled-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
