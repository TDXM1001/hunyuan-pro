<script setup lang="ts">
import type { DictAddForm, DictRecord } from '#/api/system/dict';
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
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  addDict,
  batchDeleteDicts,
  deleteDict,
  queryDictPage,
  toggleDictDisabled,
  updateDict,
} from '#/api/system/dict';

import DictDataDrawerPanel from './components/dict-data-drawer.vue';

defineOptions({ name: 'SystemSupportDictIndex' });

interface DictFormModel extends DictAddForm {
  dictId?: number;
}

const dictLoading = ref(false);
const showDictSearchBar = ref(true);
const dictSearchKeyword = ref('');
const dictSearchDisabledFlag = ref<boolean>();
const dictRows = ref<DictRecord[]>([]);
const selectedDictRows = ref<DictRecord[]>([]);
const drawerDict = ref<DictRecord>();

const dictDialogVisible = ref(false);
const dictDialogMode = ref<'add' | 'edit'>('add');
const dictFormRef = ref<FormInstance>();

const dictPagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const dictFormData = reactive<DictFormModel>({
  dictCode: '',
  dictName: '',
  remark: '',
});

const [DictDataDrawer, dictDataDrawerApi] = useVbenDrawer({
  connectedComponent: DictDataDrawerPanel,
  destroyOnClose: false,
});

const dictRules: FormRules<DictFormModel> = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
};

const dictColumnsFactory = (): ColumnOption<DictRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'dictName', label: '字典名称', minWidth: 180 },
  {
    prop: 'dictCode',
    label: '字典编码',
    minWidth: 180,
    useSlot: true,
  },
  {
    prop: 'disabledFlag',
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
    width: 220,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns: dictColumns, columnChecks: dictColumnChecks } =
  useTableColumns(dictColumnsFactory);

const hasPagination = computed(() => dictPagination.total > dictPagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);
const dictDialogTitle = computed(() =>
  dictDialogMode.value === 'add' ? '新增字典' : '编辑字典',
);

function resetDictForm() {
  Object.assign(dictFormData, {
    dictCode: '',
    dictName: '',
    remark: '',
  });
  dictFormData.dictId = undefined;
}

function syncDrawerDict(rows: DictRecord[]) {
  if (!drawerDict.value?.dictId) {
    return;
  }

  const nextDrawerDict = rows.find((item) => item.dictId === drawerDict.value?.dictId);

  if (!nextDrawerDict) {
    drawerDict.value = undefined;
    void dictDataDrawerApi.close();
    return;
  }

  drawerDict.value = nextDrawerDict;
}

async function loadDictPage() {
  dictLoading.value = true;
  try {
    const result = await queryDictPage({
      disabledFlag: dictSearchDisabledFlag.value,
      keywords: dictSearchKeyword.value,
      pageNum: dictPagination.current,
      pageSize: dictPagination.size,
    });
    dictRows.value = result?.list ?? [];
    dictPagination.total = result?.total ?? 0;
    syncDrawerDict(dictRows.value);
  } finally {
    dictLoading.value = false;
  }
}

function handleDictSearch() {
  dictPagination.current = 1;
  void loadDictPage();
}

function handleDictReset() {
  dictSearchKeyword.value = '';
  dictSearchDisabledFlag.value = undefined;
  dictPagination.current = 1;
  void loadDictPage();
}

function handleToggleDictSearchBar() {
  showDictSearchBar.value = !showDictSearchBar.value;
}

function handleDictSelectionChange(rows: DictRecord[]) {
  selectedDictRows.value = rows;
}

function openAddDictDialog() {
  dictDialogMode.value = 'add';
  resetDictForm();
  dictDialogVisible.value = true;
}

function openEditDictDialog(row: DictRecord) {
  dictDialogMode.value = 'edit';
  Object.assign(dictFormData, {
    dictCode: row.dictCode,
    dictId: row.dictId,
    dictName: row.dictName,
    remark: row.remark || '',
  });
  dictDialogVisible.value = true;
}

function openDictDataDrawer(row: DictRecord) {
  drawerDict.value = row;
  dictDataDrawerApi.open();
}

async function handleSubmitDict() {
  const valid = await dictFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dictDialogMode.value === 'add') {
    await addDict({
      dictCode: dictFormData.dictCode,
      dictName: dictFormData.dictName,
      remark: dictFormData.remark,
    });
    ElMessage.success('新增字典成功');
  } else {
    await updateDict({
      dictCode: dictFormData.dictCode,
      dictId: dictFormData.dictId as number,
      dictName: dictFormData.dictName,
      remark: dictFormData.remark,
    });
    ElMessage.success('更新字典成功');
  }

  dictDialogVisible.value = false;
  await loadDictPage();
}

async function handleToggleDictDisabled(row: DictRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.disabledFlag ? '启用' : '停用'}字典“${row.dictName}”吗？`,
      '状态确认',
      { type: 'warning' },
    );
    await toggleDictDisabled(row.dictId);
    ElMessage.success('字典状态已更新');
    await loadDictPage();
  } catch {
    // 用户取消
  }
}

async function handleDeleteDict(row: DictRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除字典“${row.dictName}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await deleteDict(row.dictId);
    ElMessage.success('字典删除成功');
    await loadDictPage();
  } catch {
    // 用户取消
  }
}

async function handleBatchDeleteDicts() {
  if (selectedDictRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的字典');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDictRows.value.length} 个字典吗？`,
      '批量删除确认',
      { type: 'warning' },
    );

    const deletingIds = new Set(selectedDictRows.value.map((item) => item.dictId));
    await batchDeleteDicts([...deletingIds]);
    selectedDictRows.value = [];

    if (drawerDict.value?.dictId && deletingIds.has(drawerDict.value.dictId)) {
      drawerDict.value = undefined;
      await dictDataDrawerApi.close();
    }

    ElMessage.success('批量删除字典成功');
    await loadDictPage();
  } catch {
    // 用户取消
  }
}

function handleDictPageCurrentChange(value: number) {
  dictPagination.current = value;
  void loadDictPage();
}

function handleDictPageSizeChange(value: number) {
  dictPagination.size = value;
  dictPagination.current = 1;
  void loadDictPage();
}

onMounted(() => {
  void loadDictPage().catch((error) => {
    ElMessage.error(error?.message || '数据字典加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="dict-page">
      <ElCard
        v-show="showDictSearchBar"
        class="dict-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :collapsible="false"
          :loading="dictLoading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleDictReset"
          @search="handleDictSearch"
        >
          <ElFormItem class="dict-page__keyword-item" label="关键字">
            <ElInput
              v-model="dictSearchKeyword"
              clearable
              placeholder="请输入字典名称或编码"
              @keyup.enter="handleDictSearch"
            />
          </ElFormItem>
          <ElFormItem class="dict-page__status-item" label="状态">
            <ElSelect
              v-model="dictSearchDisabledFlag"
              clearable
              placeholder="请选择状态"
            >
              <ElOption :value="false" label="启用" />
              <ElOption :value="true" label="禁用" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="dict-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="dictColumnChecks"
            :loading="dictLoading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showDictSearchBar"
            @search="handleToggleDictSearchBar"
          >
            <template #left>
              <ElSpace>
                <ElButton type="primary" @click="openAddDictDialog">
                  新增字典
                </ElButton>
                <ElButton
                  :disabled="selectedDictRows.length === 0"
                  @click="handleBatchDeleteDicts"
                >
                  批量删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="dictColumns"
            :data="dictRows"
            :height="tableHeight"
            :loading="dictLoading"
            :pagination="dictPagination"
            :pagination-options="{
              align: 'center',
              hideOnSinglePage: false,
              layout: 'sizes, prev, pager, next, jumper',
              pageSizes: [10, 20, 30],
              showTotalSummary: true,
              size: 'small',
            }"
            row-key="dictId"
            @pagination:current-change="handleDictPageCurrentChange"
            @pagination:size-change="handleDictPageSizeChange"
            @selection-change="handleDictSelectionChange"
          >
            <template #dictCode="{ row }">
              <button
                class="dict-page__code-link"
                type="button"
                @click="openDictDataDrawer(row)"
              >
                {{ row.dictCode }}
              </button>
            </template>

            <template #disabledFlag="{ row }">
              <ElTag
                :type="row.disabledFlag ? 'danger' : 'success'"
                effect="plain"
                size="small"
              >
                {{ row.disabledFlag ? '禁用' : '启用' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="dict-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openDictDataDrawer(row)"
                >
                  字典值
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openEditDictDialog(row)"
                >
                  编辑
                </ElButton>
                <ElButton
                  link
                  size="small"
                  :type="row.disabledFlag ? 'success' : 'warning'"
                  @click="handleToggleDictDisabled(row)"
                >
                  {{ row.disabledFlag ? '启用' : '停用' }}
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="danger"
                  @click="handleDeleteDict(row)"
                >
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <DictDataDrawer :dict="drawerDict" />

      <ElDialog
        v-model="dictDialogVisible"
        :title="dictDialogTitle"
        width="520px"
        @closed="resetDictForm"
      >
        <ElForm
          ref="dictFormRef"
          :model="dictFormData"
          :rules="dictRules"
          label-position="top"
        >
          <ElFormItem label="字典名称" prop="dictName">
            <ElInput v-model="dictFormData.dictName" placeholder="请输入字典名称" />
          </ElFormItem>
          <ElFormItem label="字典编码" prop="dictCode">
            <ElInput v-model="dictFormData.dictCode" placeholder="请输入字典编码" />
          </ElFormItem>
          <ElFormItem label="备注" prop="remark">
            <ElInput
              v-model="dictFormData.remark"
              maxlength="255"
              placeholder="请输入备注"
              type="textarea"
            />
          </ElFormItem>
        </ElForm>

        <template #footer>
          <ElSpace>
            <ElButton @click="dictDialogVisible = false">取消</ElButton>
            <ElButton type="primary" @click="handleSubmitDict">保存</ElButton>
          </ElSpace>
        </template>
      </ElDialog>
    </div>
  </Page>
</template>

<style scoped>
.dict-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.dict-page__search-card,
.dict-page__table-card {
  border-radius: 8px;
}

.dict-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.dict-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.dict-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.dict-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.dict-page :deep(.art-table-panel),
.dict-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.dict-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.dict-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.dict-page__keyword-item :deep(.el-form-item__content) {
  width: 220px;
}

.dict-page__status-item :deep(.el-form-item__content) {
  width: 140px;
}

.dict-page__code-link {
  background: none;
  border: 0;
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 14px;
  line-height: 22px;
  padding: 0;
  text-align: left;
}

.dict-page__code-link:hover {
  text-decoration: underline;
}

.dict-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.dict-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.dict-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .dict-page__keyword-item :deep(.el-form-item__content),
  .dict-page__status-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
