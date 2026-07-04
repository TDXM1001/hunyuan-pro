<script setup lang="ts">
import type {
  SerialNumberDefinition,
  SerialNumberGenerateParams,
} from '#/api/system/serial-number';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, onMounted, reactive, ref, watch } from 'vue';

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
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  generateSerialNumbers,
  querySerialNumberList,
} from '#/api/system/serial-number';

import SerialNumberRecordDrawerPanel from './components/serial-number-record-drawer.vue';

defineOptions({ name: 'SystemSupportSerialNumberList' });

const ruleTypeOptions = [
  { label: '无周期', value: '' },
  { label: '年', value: '[yyyy]' },
  { label: '年月', value: '[mm]' },
  { label: '年月日', value: '[dd]' },
] as const;

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<SerialNumberDefinition[]>([]);
const selectedSerialNumber = ref<SerialNumberDefinition>();
const generateDialogVisible = ref(false);
const generateFormRef = ref<FormInstance>();

const [SerialNumberRecordDrawer, serialNumberRecordDrawerApi] = useVbenDrawer({
  connectedComponent: SerialNumberRecordDrawerPanel,
  destroyOnClose: false,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchKeyword = ref('');
const searchRuleType = ref('');

const generateForm = reactive<SerialNumberGenerateParams>({
  count: 1,
  serialNumberId: 0,
});

const generateRules: FormRules<SerialNumberGenerateParams> = {
  count: [{ required: true, message: '请输入生成数量', trigger: 'change' }],
};

const columnsFactory = (): ColumnOption<SerialNumberDefinition>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'serialNumberId',
    label: 'serialNumberId',
    width: 120,
    align: 'center',
  },
  { prop: 'businessName', label: 'businessName', minWidth: 180 },
  { prop: 'format', label: 'format', minWidth: 200 },
  {
    prop: 'ruleType',
    label: 'ruleType',
    width: 120,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'initNumber',
    label: '初始值',
    width: 110,
    align: 'center',
    formatter: (row) => (row.initNumber == null ? '-' : `${row.initNumber}`),
  },
  {
    prop: 'lastNumber',
    label: 'lastNumber',
    width: 120,
    align: 'center',
    formatter: (row) => (row.lastNumber == null ? '-' : `${row.lastNumber}`),
  },
  {
    prop: 'lastTime',
    label: '最后生成时间',
    minWidth: 180,
    formatter: (row) => row.lastTime || '-',
  },
  {
    prop: 'actions',
    label: '操作',
    width: 180,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const filteredRows = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return rows.value.filter((item) => {
    const matchKeyword
      = !keyword
      || [
        item.serialNumberId,
        item.businessName,
        item.format,
        item.ruleType || '',
        item.remark || '',
      ]
        .join(' ')
        .toLowerCase()
        .includes(keyword);
    const matchRuleType
      = !searchRuleType.value || item.ruleType === searchRuleType.value;

    return matchKeyword && matchRuleType;
  });
});

const displayRows = computed(() => {
  const start = (pagination.current - 1) * pagination.size;
  return filteredRows.value.slice(start, start + pagination.size);
});

function resolveRuleTypeLabel(value?: null | string) {
  return ruleTypeOptions.find((item) => item.value === (value || ''))?.label || '-';
}

function resetGenerateForm() {
  Object.assign(generateForm, {
    count: 1,
    serialNumberId: selectedSerialNumber.value?.serialNumberId || 0,
  });
}

async function loadData() {
  loading.value = true;
  try {
    rows.value = (await querySerialNumberList()) ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
}

function handleReset() {
  searchKeyword.value = '';
  searchRuleType.value = '';
  pagination.current = 1;
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openRecordDrawer(row: SerialNumberDefinition) {
  selectedSerialNumber.value = row;
  serialNumberRecordDrawerApi.open();
}

function openGenerateDialog(row: SerialNumberDefinition) {
  selectedSerialNumber.value = row;
  generateDialogVisible.value = true;
  resetGenerateForm();
}

async function handleGenerate() {
  const valid = await generateFormRef.value?.validate().catch(() => false);
  if (!valid || !selectedSerialNumber.value) {
    return;
  }

  const result = await generateSerialNumbers({
    count: generateForm.count,
    serialNumberId: selectedSerialNumber.value.serialNumberId,
  });

  generateDialogVisible.value = false;
  ElMessage.success(`手动生成成功，本次返回 ${result?.length ?? 0} 个序列号`);
  await loadData();
}

function handleCurrentChange(value: number) {
  pagination.current = value;
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
}

watch(
  filteredRows,
  (value) => {
    pagination.total = value.length;
    const maxPage = Math.max(1, Math.ceil(value.length / pagination.size));
    if (pagination.current > maxPage) {
      pagination.current = maxPage;
    }
  },
  { immediate: true },
);

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="serial-number-page">
      <ElCard
        v-show="showSearchBar"
        class="serial-number-page__search-card"
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
          <ElFormItem class="serial-number-page__keyword-item" label="关键字">
            <ElInput
              v-model="searchKeyword"
              clearable
              placeholder="请输入业务名称、格式或 ID"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="serial-number-page__rule-type-item" label="规则周期">
            <ElSelect
              v-model="searchRuleType"
              clearable
              placeholder="请选择规则周期"
            >
              <ElOption
                v-for="item in ruleTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="serial-number-page__table-card" shadow="never">
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
            :data="displayRows"
            :height="'100%'"
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
            row-key="serialNumberId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #ruleType="{ row }">
              <ElTag effect="plain" size="small" type="primary">
                {{ resolveRuleTypeLabel(row.ruleType) }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="serial-number-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openRecordDrawer(row)"
                >
                  生成记录
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openGenerateDialog(row)"
                >
                  手动生成
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <SerialNumberRecordDrawer :serial-number="selectedSerialNumber" />

      <ElDialog
        v-model="generateDialogVisible"
        title="手动生成"
        width="520px"
        @closed="resetGenerateForm"
      >
        <ElForm
          ref="generateFormRef"
          :model="generateForm"
          :rules="generateRules"
          label-position="top"
        >
          <ElFormItem label="业务名称">
            <ElInput
              :model-value="selectedSerialNumber?.businessName || ''"
              disabled
            />
          </ElFormItem>
          <ElFormItem label="format">
            <ElInput :model-value="selectedSerialNumber?.format || ''" disabled />
          </ElFormItem>
          <ElFormItem label="生成数量" prop="count">
            <ElInputNumber
              v-model="generateForm.count"
              :min="1"
              :max="50"
              style="width: 100%"
            />
          </ElFormItem>
        </ElForm>

        <template #footer>
          <ElSpace>
            <ElButton @click="generateDialogVisible = false">取消</ElButton>
            <ElButton type="primary" @click="handleGenerate">生成</ElButton>
          </ElSpace>
        </template>
      </ElDialog>
    </div>
  </Page>
</template>

<style scoped>
.serial-number-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.serial-number-page__search-card,
.serial-number-page__table-card {
  border-radius: 8px;
}

.serial-number-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.serial-number-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.serial-number-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.serial-number-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.serial-number-page :deep(.art-table-panel),
.serial-number-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.serial-number-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.serial-number-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.serial-number-page__keyword-item :deep(.el-form-item__content) {
  width: 220px;
}

.serial-number-page__rule-type-item :deep(.el-form-item__content) {
  width: 168px;
}

.serial-number-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.serial-number-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.serial-number-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .serial-number-page__keyword-item :deep(.el-form-item__content),
  .serial-number-page__rule-type-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
