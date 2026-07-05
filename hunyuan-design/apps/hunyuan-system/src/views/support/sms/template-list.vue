<script setup lang="ts">
import type {
  SmsTemplateAddForm,
  SmsTemplateRecord,
  SmsTemplateUpdateForm,
} from '#/api/system/sms';
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
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addSmsTemplate,
  querySmsTemplatePage,
  updateSmsTemplate,
  updateSmsTemplateDisabled,
} from '#/api/system/sms';

defineOptions({ name: 'SystemSupportSmsTemplateList' });

interface SmsTemplateFormModel extends SmsTemplateAddForm {
  templateCode: string;
}

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<SmsTemplateRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  disableFlag: undefined as boolean | undefined,
  templateCode: '',
  templateName: '',
});

const formData = reactive<SmsTemplateFormModel>({
  disableFlag: false,
  remark: '',
  templateCode: '',
  templateContent: '',
  templateName: '',
});

const rules: FormRules<SmsTemplateFormModel> = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateContent: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<SmsTemplateRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'templateCode', label: '模板编码', minWidth: 180 },
  { prop: 'templateName', label: '模板名称', minWidth: 180 },
  {
    prop: 'templateContent',
    label: '模板内容',
    minWidth: 320,
    formatter: (row) => row.templateContent || '-',
  },
  {
    prop: 'disableFlag',
    label: '状态',
    width: 90,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 180,
    formatter: (row) => row.remark || '-',
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 160,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const dialogTitle = computed(() =>
  dialogMode.value === 'add' ? '新增短信模板' : '编辑短信模板',
);
const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resetForm() {
  Object.assign(formData, {
    disableFlag: false,
    remark: '',
    templateCode: '',
    templateContent: '',
    templateName: '',
  });
}

async function loadData() {
  loading.value = true;
  try {
    const result = await querySmsTemplatePage({
      disableFlag: searchForm.disableFlag,
      pageNum: pagination.current,
      pageSize: pagination.size,
      templateCode: searchForm.templateCode,
      templateName: searchForm.templateName,
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
    disableFlag: undefined,
    templateCode: '',
    templateName: '',
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

// 编辑时保留原模板编码，避免把后端主键误改成新的值。
function openEditDialog(row: SmsTemplateRecord) {
  dialogMode.value = 'edit';
  Object.assign(formData, {
    disableFlag: row.disableFlag ?? false,
    remark: row.remark || '',
    templateCode: row.templateCode,
    templateContent: row.templateContent,
    templateName: row.templateName,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addSmsTemplate(formData as SmsTemplateAddForm);
    ElMessage.success('新增短信模板成功');
  } else {
    await updateSmsTemplate(formData as SmsTemplateUpdateForm);
    ElMessage.success('更新短信模板成功');
  }

  dialogVisible.value = false;
  await loadData();
}

async function handleToggleDisabled(row: SmsTemplateRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.disableFlag ? '启用' : '停用'}模板“${row.templateName}”吗？`,
      '状态确认',
      { type: 'warning' },
    );
    await updateSmsTemplateDisabled(row.templateCode, !row.disableFlag);
    ElMessage.success('模板状态已更新');
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
    <div class="template-page">
      <ElCard
        v-show="showSearchBar"
        class="template-page__search-card"
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
          <ElFormItem class="template-page__code-item" label="模板编码">
            <ElInput
              v-model="searchForm.templateCode"
              clearable
              placeholder="请输入模板编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="template-page__name-item" label="模板名称">
            <ElInput
              v-model="searchForm.templateName"
              clearable
              placeholder="请输入模板名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="template-page__status-item" label="状态">
            <ElSelect
              v-model="searchForm.disableFlag"
              clearable
              placeholder="请选择状态"
            >
              <ElOption :value="false" label="启用" />
              <ElOption :value="true" label="禁用" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="template-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">新增模板</ElButton>
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
            row-key="templateCode"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #disableFlag="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="row.disableFlag ? 'danger' : 'success'"
              >
                {{ row.disableFlag ? '禁用' : '启用' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="template-page__actions">
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
                  :type="row.disableFlag ? 'success' : 'warning'"
                  @click="handleToggleDisabled(row)"
                >
                  {{ row.disableFlag ? '启用' : '停用' }}
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <ElDialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="680px"
        @closed="resetForm"
      >
        <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
          <ElFormItem label="模板编码" prop="templateCode">
            <ElInput
              v-model="formData.templateCode"
              :disabled="dialogMode === 'edit'"
              placeholder="请输入模板编码"
            />
          </ElFormItem>
          <ElFormItem label="模板名称" prop="templateName">
            <ElInput v-model="formData.templateName" placeholder="请输入模板名称" />
          </ElFormItem>
          <ElFormItem label="模板内容" prop="templateContent">
            <ElInput
              v-model="formData.templateContent"
              :rows="5"
              placeholder="请输入模板内容"
              type="textarea"
            />
          </ElFormItem>
          <ElFormItem label="是否禁用" prop="disableFlag">
            <ElSwitch
              v-model="formData.disableFlag"
              inline-prompt
              active-text="禁用"
              inactive-text="启用"
            />
          </ElFormItem>
          <ElFormItem label="备注" prop="remark">
            <ElInput
              v-model="formData.remark"
              :rows="4"
              maxlength="500"
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
.template-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.template-page__search-card,
.template-page__table-card {
  border-radius: 8px;
}

.template-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.template-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.template-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.template-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.template-page :deep(.art-table-panel),
.template-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.template-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.template-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.template-page__code-item :deep(.el-form-item__content),
.template-page__name-item :deep(.el-form-item__content) {
  width: 220px;
}

.template-page__status-item :deep(.el-form-item__content) {
  width: 168px;
}

.template-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.template-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.template-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .template-page__code-item :deep(.el-form-item__content),
  .template-page__name-item :deep(.el-form-item__content),
  .template-page__status-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
