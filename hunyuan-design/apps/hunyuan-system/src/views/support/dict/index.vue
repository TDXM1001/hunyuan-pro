<script setup lang="ts">
import type {
  DictAddForm,
  DictDataAddForm,
  DictDataRecord,
  DictRecord,
} from '#/api/system/dict';
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
  ElEmpty,
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
  addDict,
  addDictData,
  batchDeleteDictData,
  batchDeleteDicts,
  deleteDict,
  deleteDictData,
  queryDictDataList,
  queryDictPage,
  toggleDictDataDisabled,
  toggleDictDisabled,
  updateDict,
  updateDictData,
} from '#/api/system/dict';

defineOptions({ name: 'SystemSupportDictIndex' });

interface DictFormModel extends DictAddForm {
  dictId?: number;
}

interface DictDataFormModel extends DictDataAddForm {
  dictCode?: string;
  dictDataId?: number;
}

const dictDataStyleOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'danger' },
] as const;

const dictLoading = ref(false);
const dictDataLoading = ref(false);
const showDictSearchBar = ref(true);
const dictSearchKeyword = ref('');
const dictSearchDisabledFlag = ref<boolean>();
const dictRows = ref<DictRecord[]>([]);
const selectedDictRows = ref<DictRecord[]>([]);
const activeDictId = ref<null | number>(null);
const activeDict = ref<DictRecord>();
const dictDataRows = ref<DictDataRecord[]>([]);
const selectedDictDataRows = ref<DictDataRecord[]>([]);

const dictDialogVisible = ref(false);
const dictDialogMode = ref<'add' | 'edit'>('add');
const dictFormRef = ref<FormInstance>();

const dictDataDialogVisible = ref(false);
const dictDataDialogMode = ref<'add' | 'edit'>('add');
const dictDataFormRef = ref<FormInstance>();

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

const dictDataFormData = reactive<DictDataFormModel>({
  dictCode: '',
  dictId: 0,
  dataLabel: '',
  dataStyle: 'default',
  dataValue: '',
  remark: '',
  sortOrder: 100,
});

const dictRules: FormRules<DictFormModel> = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
};

const dictDataRules: FormRules<DictDataFormModel> = {
  dataLabel: [{ required: true, message: '请输入字典项名称', trigger: 'blur' }],
  dataValue: [{ required: true, message: '请输入字典项值', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
};

const dictColumnsFactory = (): ColumnOption<DictRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'dictName',
    label: '字典名称',
    minWidth: 160,
    useSlot: true,
  },
  { prop: 'dictCode', label: '字典编码', minWidth: 150 },
  {
    prop: 'disabledFlag',
    label: '状态',
    width: 90,
    align: 'center',
    useSlot: true,
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 180,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const dictDataColumnsFactory = (): ColumnOption<DictDataRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'dataLabel', label: '字典项名称', minWidth: 150 },
  { prop: 'dataValue', label: '字典项值', minWidth: 140 },
  {
    prop: 'dataStyle',
    label: '样式',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  { prop: 'sortOrder', label: '排序', width: 80, align: 'center' },
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
    width: 180,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns: dictColumns, columnChecks: dictColumnChecks } =
  useTableColumns(dictColumnsFactory);
const { columns: dictDataColumns, columnChecks: dictDataColumnChecks } =
  useTableColumns(dictDataColumnsFactory);

const dictHasPagination = computed(() => dictPagination.total > dictPagination.size);
const dictTableHeight = computed(() =>
  dictHasPagination.value ? 'calc(100% - 44px)' : '100%',
);
const hasActiveDict = computed(() => Boolean(activeDict.value?.dictId));
const dictDataDialogTitle = computed(() =>
  dictDataDialogMode.value === 'add' ? '新增字典项' : '编辑字典项',
);
const dictDialogTitle = computed(() =>
  dictDialogMode.value === 'add' ? '新增字典' : '编辑字典',
);

function resolveDataStyleTagType(
  style?: null | string,
): 'danger' | 'info' | 'primary' | 'success' | 'warning' | undefined {
  if (!style || style === 'default') {
    return undefined;
  }
  if (
    style === 'danger'
    || style === 'info'
    || style === 'primary'
    || style === 'success'
    || style === 'warning'
  ) {
    return style;
  }
  return undefined;
}

function getDataStyleLabel(style?: null | string) {
  return (
    dictDataStyleOptions.find((item) => item.value === style)?.label
    || '默认'
  );
}

function resetDictForm() {
  Object.assign(dictFormData, {
    dictCode: '',
    dictName: '',
    remark: '',
  });
  dictFormData.dictId = undefined;
}

function resetDictDataForm() {
  Object.assign(dictDataFormData, {
    dictCode: activeDict.value?.dictCode || '',
    dictId: activeDict.value?.dictId || 0,
    dataLabel: '',
    dataStyle: 'default',
    dataValue: '',
    remark: '',
    sortOrder: 100,
  });
  dictDataFormData.dictDataId = undefined;
}

async function loadDictData() {
  if (!activeDict.value?.dictId) {
    dictDataRows.value = [];
    selectedDictDataRows.value = [];
    return;
  }

  dictDataLoading.value = true;
  try {
    dictDataRows.value =
      (await queryDictDataList(activeDict.value.dictId)) ?? [];
    selectedDictDataRows.value = [];
  } finally {
    dictDataLoading.value = false;
  }
}

async function syncActiveDict(rows: DictRecord[]) {
  const nextActiveDict =
    rows.find((item) => item.dictId === activeDictId.value) ?? rows[0];

  if (!nextActiveDict) {
    activeDictId.value = null;
    activeDict.value = undefined;
    dictDataRows.value = [];
    selectedDictDataRows.value = [];
    return;
  }

  activeDictId.value = nextActiveDict.dictId;
  activeDict.value = nextActiveDict;
  await loadDictData();
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
    await syncActiveDict(dictRows.value);
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

function handleDictDataSelectionChange(rows: DictDataRecord[]) {
  selectedDictDataRows.value = rows;
}

function handleDictCurrentChange(row: DictRecord) {
  if (activeDictId.value === row.dictId) {
    return;
  }

  activeDictId.value = row.dictId;
  activeDict.value = row;
  void loadDictData();
}

function getDictRowClassName({ row }: { row: DictRecord }) {
  return row.dictId === activeDictId.value ? 'dict-page__dict-row--active' : '';
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

function openAddDictDataDialog() {
  if (!activeDict.value?.dictId) {
    ElMessage.warning('请先选择左侧字典');
    return;
  }

  dictDataDialogMode.value = 'add';
  resetDictDataForm();
  dictDataDialogVisible.value = true;
}

function openEditDictDataDialog(row: DictDataRecord) {
  dictDataDialogMode.value = 'edit';
  Object.assign(dictDataFormData, {
    dictCode: row.dictCode,
    dictDataId: row.dictDataId,
    dictId: row.dictId,
    dataLabel: row.dataLabel,
    dataStyle: row.dataStyle || 'default',
    dataValue: row.dataValue,
    remark: row.remark || '',
    sortOrder: row.sortOrder,
  });
  dictDataDialogVisible.value = true;
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

async function handleSubmitDictData() {
  const valid = await dictDataFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dictDataDialogMode.value === 'add') {
    await addDictData({
      dictId: dictDataFormData.dictId,
      dataLabel: dictDataFormData.dataLabel,
      dataStyle: dictDataFormData.dataStyle,
      dataValue: dictDataFormData.dataValue,
      remark: dictDataFormData.remark,
      sortOrder: dictDataFormData.sortOrder,
    });
    ElMessage.success('新增字典项成功');
  } else {
    await updateDictData({
      dictCode: dictDataFormData.dictCode as string,
      dictDataId: dictDataFormData.dictDataId as number,
      dictId: dictDataFormData.dictId,
      dataLabel: dictDataFormData.dataLabel,
      dataStyle: dictDataFormData.dataStyle,
      dataValue: dictDataFormData.dataValue,
      remark: dictDataFormData.remark,
      sortOrder: dictDataFormData.sortOrder,
    });
    ElMessage.success('更新字典项成功');
  }

  dictDataDialogVisible.value = false;
  await loadDictData();
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
    await batchDeleteDicts(selectedDictRows.value.map((item) => item.dictId));
    ElMessage.success('批量删除字典成功');
    selectedDictRows.value = [];
    await loadDictPage();
  } catch {
    // 用户取消
  }
}

async function handleToggleDictDataDisabled(row: DictDataRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.disabledFlag ? '启用' : '停用'}字典项“${row.dataLabel}”吗？`,
      '状态确认',
      { type: 'warning' },
    );
    await toggleDictDataDisabled(row.dictDataId);
    ElMessage.success('字典项状态已更新');
    await loadDictData();
  } catch {
    // 用户取消
  }
}

async function handleDeleteDictData(row: DictDataRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除字典项“${row.dataLabel}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await deleteDictData(row.dictDataId);
    ElMessage.success('字典项删除成功');
    await loadDictData();
  } catch {
    // 用户取消
  }
}

async function handleBatchDeleteDictData() {
  if (selectedDictDataRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的字典项');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDictDataRows.value.length} 个字典项吗？`,
      '批量删除确认',
      { type: 'warning' },
    );
    await batchDeleteDictData(
      selectedDictDataRows.value.map((item) => item.dictDataId),
    );
    ElMessage.success('批量删除字典项成功');
    selectedDictDataRows.value = [];
    await loadDictData();
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
      <section class="dict-page__types-column">
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

        <ElCard class="dict-page__types-card" shadow="never">
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
              :height="dictTableHeight"
              highlight-current-row
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
              :row-class-name="getDictRowClassName"
              row-key="dictId"
              @pagination:current-change="handleDictPageCurrentChange"
              @pagination:size-change="handleDictPageSizeChange"
              @selection-change="handleDictSelectionChange"
            >
              <template #dictName="{ row }">
                <button
                  class="dict-page__dict-link"
                  type="button"
                  @click="handleDictCurrentChange(row)"
                >
                  {{ row.dictName }}
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
                    @click="handleDictCurrentChange(row)"
                  >
                    查看项
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
      </section>

      <section class="dict-page__data-column">
        <ElCard class="dict-page__data-card" shadow="never">
          <ArtTablePanel>
            <ArtTableHeader
              v-model="dictDataColumnChecks"
              :loading="dictDataLoading"
              layout="fullscreen,columns,settings"
            >
              <template #left>
                <div class="dict-page__data-header">
                  <div v-if="activeDict" class="dict-page__data-context">
                    <span class="dict-page__data-title">{{ activeDict.dictName }}</span>
                    <ElTag effect="plain" size="small">
                      {{ activeDict.dictCode }}
                    </ElTag>
                    <ElTag
                      v-if="activeDict.disabledFlag"
                      effect="plain"
                      size="small"
                      type="danger"
                    >
                      已禁用
                    </ElTag>
                  </div>
                  <ElSpace>
                    <ElButton
                      :disabled="!hasActiveDict"
                      type="primary"
                      @click="openAddDictDataDialog"
                    >
                      新增字典项
                    </ElButton>
                    <ElButton
                      :disabled="selectedDictDataRows.length === 0"
                      @click="handleBatchDeleteDictData"
                    >
                      批量删除
                    </ElButton>
                  </ElSpace>
                </div>
              </template>
            </ArtTableHeader>

            <div v-if="!hasActiveDict" class="dict-page__empty-wrap">
              <ElEmpty description="请选择左侧字典" :image-size="96" />
            </div>

            <ArtTable
              v-else
              :columns="dictDataColumns"
              :data="dictDataRows"
              height="100%"
              :loading="dictDataLoading"
              row-key="dictDataId"
              @selection-change="handleDictDataSelectionChange"
            >
              <template #dataStyle="{ row }">
                <ElTag
                  effect="plain"
                  size="small"
                  :type="resolveDataStyleTagType(row.dataStyle)"
                >
                  {{ getDataStyleLabel(row.dataStyle) }}
                </ElTag>
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
                    @click="openEditDictDataDialog(row)"
                  >
                    编辑
                  </ElButton>
                  <ElButton
                    link
                    size="small"
                    :type="row.disabledFlag ? 'success' : 'warning'"
                    @click="handleToggleDictDataDisabled(row)"
                  >
                    {{ row.disabledFlag ? '启用' : '停用' }}
                  </ElButton>
                  <ElButton
                    link
                    size="small"
                    type="danger"
                    @click="handleDeleteDictData(row)"
                  >
                    删除
                  </ElButton>
                </ElSpace>
              </template>
            </ArtTable>
          </ArtTablePanel>
        </ElCard>
      </section>
    </div>

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

    <ElDialog
      v-model="dictDataDialogVisible"
      :title="dictDataDialogTitle"
      width="560px"
      @closed="resetDictDataForm"
    >
      <ElForm
        ref="dictDataFormRef"
        :model="dictDataFormData"
        :rules="dictDataRules"
        label-position="top"
      >
        <ElFormItem label="字典项名称" prop="dataLabel">
          <ElInput
            v-model="dictDataFormData.dataLabel"
            placeholder="请输入字典项名称"
          />
        </ElFormItem>
        <ElFormItem label="字典项值" prop="dataValue">
          <ElInput
            v-model="dictDataFormData.dataValue"
            placeholder="请输入字典项值"
          />
        </ElFormItem>
        <ElFormItem label="样式" prop="dataStyle">
          <ElSelect v-model="dictDataFormData.dataStyle" placeholder="请选择样式">
            <ElOption
              v-for="item in dictDataStyleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="排序" prop="sortOrder">
          <ElInputNumber
            v-model="dictDataFormData.sortOrder"
            :min="0"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="备注" prop="remark">
          <ElInput
            v-model="dictDataFormData.remark"
            maxlength="255"
            placeholder="请输入备注"
            type="textarea"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dictDataDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmitDictData">保存</ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.dict-page {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.dict-page__types-column,
.dict-page__data-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

.dict-page__search-card,
.dict-page__types-card,
.dict-page__data-card {
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

.dict-page__types-card,
.dict-page__data-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.dict-page__types-card :deep(.el-card__body),
.dict-page__data-card :deep(.el-card__body) {
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

.dict-page__data-header {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  width: 100%;
}

.dict-page__data-context {
  align-items: center;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dict-page__data-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.dict-page__dict-link {
  background: none;
  border: 0;
  color: var(--el-text-color-primary);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  padding: 0;
  text-align: left;
}

.dict-page__dict-link:hover {
  color: var(--el-color-primary);
}

.dict-page__empty-wrap {
  align-items: center;
  display: flex;
  flex: 1;
  justify-content: center;
  min-height: 0;
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

.dict-page :deep(.dict-page__dict-row--active td) {
  background: var(--el-color-primary-light-9);
}

@media (width <= 768px) {
  .dict-page {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(320px, 44vh) minmax(0, 1fr);
  }

  .dict-page__keyword-item :deep(.el-form-item__content),
  .dict-page__status-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
