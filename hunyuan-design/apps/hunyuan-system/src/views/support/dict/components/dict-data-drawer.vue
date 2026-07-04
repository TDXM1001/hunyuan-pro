<script setup lang="ts">
import type {
  DictDataAddForm,
  DictDataRecord,
  DictRecord,
} from '#/api/system/dict';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, reactive, ref, watch } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { useVbenDrawer } from '@vben/common-ui';

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
  addDictData,
  batchDeleteDictData,
  deleteDictData,
  queryDictDataList,
  toggleDictDataDisabled,
  updateDictData,
} from '#/api/system/dict';

defineOptions({ name: 'SystemSupportDictDataDrawer' });

interface DictDataFormModel extends DictDataAddForm {
  dictCode?: string;
  dictDataId?: number;
}

const props = defineProps<{
  dict?: DictRecord;
}>();

const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));

const dictDataStyleOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'danger' },
] as const;

const dictDataLoading = ref(false);
const dictDataSearchKeyword = ref('');
const dictDataSearchDisabledFlag = ref<boolean>();
const dictDataRows = ref<DictDataRecord[]>([]);
const selectedDictDataRows = ref<DictDataRecord[]>([]);

const dictDataDialogVisible = ref(false);
const dictDataDialogMode = ref<'add' | 'edit'>('add');
const dictDataFormRef = ref<FormInstance>();
const lastLoadedDictId = ref<null | number>(null);

const dictDataFormData = reactive<DictDataFormModel>({
  dictCode: '',
  dictId: 0,
  dataLabel: '',
  dataStyle: 'default',
  dataValue: '',
  remark: '',
  sortOrder: 100,
});

const dictDataRules: FormRules<DictDataFormModel> = {
  dataLabel: [{ required: true, message: '请输入字典项名称', trigger: 'blur' }],
  dataValue: [{ required: true, message: '请输入字典项值', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
};

const dictDataColumnsFactory = (): ColumnOption<DictDataRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { prop: 'dataValue', label: '值', minWidth: 160 },
  { prop: 'dataLabel', label: '名称', minWidth: 160 },
  {
    prop: 'dataStyle',
    label: '样式',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'disabledFlag',
    label: '状态',
    width: 120,
    align: 'center',
    useSlot: true,
  },
  { prop: 'sortOrder', label: '排序', width: 80, align: 'center' },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 160,
    formatter: (row) => row.remark || '-',
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 120,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns: dictDataColumns, columnChecks: dictDataColumnChecks } =
  useTableColumns(dictDataColumnsFactory);

const dictDataDialogTitle = computed(() =>
  dictDataDialogMode.value === 'add' ? '新增字典项' : '编辑字典项',
);
const filteredDictDataRows = computed(() => {
  const keyword = dictDataSearchKeyword.value.trim().toLowerCase();

  return dictDataRows.value.filter((item) => {
    const matchKeyword
      = !keyword
      || [item.dataLabel, item.dataValue, item.remark || '']
        .join(' ')
        .toLowerCase()
        .includes(keyword);
    const matchDisabledFlag
      = dictDataSearchDisabledFlag.value == null
      || item.disabledFlag === dictDataSearchDisabledFlag.value;

    return matchKeyword && matchDisabledFlag;
  });
});

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

function resetDictDataFilters() {
  dictDataSearchKeyword.value = '';
  dictDataSearchDisabledFlag.value = undefined;
}

function resetDictDataForm() {
  Object.assign(dictDataFormData, {
    dictCode: props.dict?.dictCode || '',
    dictId: props.dict?.dictId || 0,
    dataLabel: '',
    dataStyle: 'default',
    dataValue: '',
    remark: '',
    sortOrder: 100,
  });
  dictDataFormData.dictDataId = undefined;
}

async function loadDictData() {
  if (!props.dict?.dictId) {
    dictDataRows.value = [];
    selectedDictDataRows.value = [];
    return;
  }

  dictDataLoading.value = true;
  try {
    dictDataRows.value = (await queryDictDataList(props.dict.dictId)) ?? [];
    selectedDictDataRows.value = [];
  } finally {
    dictDataLoading.value = false;
  }
}

function handleDictDataSearch() {
  selectedDictDataRows.value = [];
}

function handleDictDataReset() {
  resetDictDataFilters();
  selectedDictDataRows.value = [];
}

function handleDictDataSelectionChange(rows: DictDataRecord[]) {
  selectedDictDataRows.value = rows;
}

function openAddDictDataDialog() {
  if (!props.dict?.dictId) {
    ElMessage.warning('请先选择字典');
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
    selectedDictDataRows.value = [];
    ElMessage.success('批量删除字典项成功');
    await loadDictData();
  } catch {
    // 用户取消
  }
}

watch(
  () => [props.dict?.dictId, drawerOpen.value] as const,
  ([dictId, isOpen]) => {
    if (!isOpen) {
      selectedDictDataRows.value = [];
      dictDataDialogVisible.value = false;
      return;
    }

    if (!dictId) {
      dictDataRows.value = [];
      selectedDictDataRows.value = [];
      return;
    }

    if (lastLoadedDictId.value !== dictId) {
      lastLoadedDictId.value = dictId;
      resetDictDataFilters();
    }

    void loadDictData();
  },
  { immediate: true },
);
</script>

<template>
  <Drawer
    class="w-[1180px] max-w-[calc(100vw-24px)]"
    close-icon-placement="left"
    content-class="!p-0"
    :footer="false"
    title="字典值"
  >
    <div class="dict-data-drawer">
      <ElCard class="dict-data-drawer__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="dictDataLoading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleDictDataReset"
          @search="handleDictDataSearch"
        >
          <ElFormItem class="dict-data-drawer__keyword-item" label="关键字">
            <ElInput
              v-model="dictDataSearchKeyword"
              clearable
              placeholder="关键字"
              @keyup.enter="handleDictDataSearch"
            />
          </ElFormItem>
          <ElFormItem class="dict-data-drawer__status-item" label="禁用">
            <ElSelect
              v-model="dictDataSearchDisabledFlag"
              clearable
              placeholder="请选择"
            >
              <ElOption :value="false" label="启用" />
              <ElOption :value="true" label="禁用" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="dict-data-drawer__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="dictDataColumnChecks"
            :loading="dictDataLoading"
            layout=""
          >
            <template #left>
              <ElSpace>
                <ElButton type="primary" @click="openAddDictDataDialog">
                  新增
                </ElButton>
                <ElButton
                  :disabled="selectedDictDataRows.length === 0"
                  @click="handleBatchDeleteDictData"
                >
                  批量删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTableHeader>

          <div class="dict-data-drawer__table-wrap">
            <ArtTable
              :columns="dictDataColumns"
              :data="filteredDictDataRows"
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
                  {{ row.dataStyle || 'default' }}
                </ElTag>
              </template>

              <template #disabledFlag="{ row }">
                <ElSwitch
                  :model-value="!row.disabledFlag"
                  inline-prompt
                  inactive-text="禁用"
                  active-text="启用"
                  @change="handleToggleDictDataDisabled(row)"
                />
              </template>

              <template #actions="{ row }">
                <ElSpace class="dict-data-drawer__actions">
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
                    type="danger"
                    @click="handleDeleteDictData(row)"
                  >
                    删除
                  </ElButton>
                </ElSpace>
              </template>
            </ArtTable>
          </div>

          <div class="dict-data-drawer__summary">共计 {{ filteredDictDataRows.length }} 条</div>
        </ArtTablePanel>
      </ElCard>
    </div>

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
  </Drawer>
</template>

<style scoped>
.dict-data-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.dict-data-drawer__search-card,
.dict-data-drawer__table-card {
  border-radius: 8px;
}

.dict-data-drawer__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.dict-data-drawer__search-card :deep(.el-card__body) {
  padding: 16px;
}

.dict-data-drawer__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.dict-data-drawer__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.dict-data-drawer :deep(.art-table-panel) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.dict-data-drawer :deep(.art-table-header) {
  margin-bottom: 18px;
}

.dict-data-drawer :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.dict-data-drawer__table-wrap {
  flex: 1;
  min-height: 0;
}

.dict-data-drawer__summary {
  color: var(--el-text-color-regular);
  flex-shrink: 0;
  font-size: 14px;
  line-height: 22px;
  padding-top: 12px;
  text-align: right;
}

.dict-data-drawer__keyword-item :deep(.el-form-item__content) {
  width: 360px;
}

.dict-data-drawer__status-item :deep(.el-form-item__content) {
  width: 180px;
}

.dict-data-drawer__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.dict-data-drawer__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.dict-data-drawer__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .dict-data-drawer__keyword-item :deep(.el-form-item__content),
  .dict-data-drawer__status-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
