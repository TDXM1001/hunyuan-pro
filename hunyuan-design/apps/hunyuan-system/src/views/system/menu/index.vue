<script setup lang="ts">
import type {
  MenuAddForm,
  MenuRecord,
  MenuTreeRecord,
  MenuUpdateForm,
} from '#/api/system/menu';
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
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addMenu,
  batchDeleteMenus,
  queryMenuTree,
  updateMenu,
} from '#/api/system/menu';

defineOptions({ name: 'SystemMenuManagement' });

interface MenuTreeRow extends MenuRecord {
  children?: MenuTreeRow[];
  level: number;
}

interface MenuFormModel extends MenuAddForm {
  menuId?: number;
}

const MENU_TYPE_OPTIONS = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '功能点', value: 3 },
] as const;

const PERMS_TYPE_OPTIONS = [
  { label: '无需权限', value: 0 },
  { label: '前端权限', value: 1 },
  { label: '后端权限', value: 2 },
  { label: '前后端权限', value: 3 },
] as const;

const loading = ref(false);
const keyword = ref('');
const showSearchBar = ref(true);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const addParentName = ref('');
const formRef = ref<FormInstance>();
const rawTree = ref<MenuTreeRecord[]>([]);

const formData = reactive<MenuFormModel>({
  apiPerms: '',
  cacheFlag: false,
  component: '',
  contextMenuId: null,
  disabledFlag: false,
  frameFlag: false,
  frameUrl: '',
  icon: '',
  menuName: '',
  menuType: 2,
  parentId: 0,
  path: '',
  permsType: 0,
  sort: 100,
  visibleFlag: true,
  webPerms: '',
});

const rules: FormRules<MenuFormModel> = {
  cacheFlag: [{ required: true, message: '请选择是否缓存', trigger: 'change' }],
  disabledFlag: [{ required: true, message: '请选择禁用状态', trigger: 'change' }],
  frameFlag: [{ required: true, message: '请选择是否外链', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  visibleFlag: [{ required: true, message: '请选择显示状态', trigger: 'change' }],
};

const columnsFactory = (): ColumnOption<MenuTreeRow>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'menuName', label: '菜单名称', minWidth: 240, useSlot: true },
  { prop: 'path', label: '路由路径', minWidth: 180, formatter: (row) => row.path || '-' },
  {
    prop: 'component',
    label: '组件路径',
    minWidth: 220,
    formatter: (row) => row.component || '-',
  },
  {
    prop: 'webPerms',
    label: '前端权限码',
    minWidth: 180,
    formatter: (row) => row.webPerms || '-',
  },
  {
    prop: 'apiPerms',
    label: '后端权限码',
    minWidth: 180,
    formatter: (row) => row.apiPerms || '-',
  },
  { prop: 'sort', label: '排序', width: 90, align: 'center' },
  { prop: 'status', label: '状态', width: 150, align: 'center', useSlot: true },
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

function buildMenuRows(nodes: MenuTreeRecord[], level = 0): MenuTreeRow[] {
  return nodes.map((node) => {
    const children = buildMenuRows(node.children ?? [], level + 1);
    return {
      ...node,
      children: children.length > 0 ? children : undefined,
      level,
      parentId: node.parentId ?? 0,
      sort: node.sort ?? 0,
    };
  });
}

function flattenMenuRows(nodes: MenuTreeRow[]): MenuTreeRow[] {
  return nodes.flatMap((node) => {
    const { children, ...current } = node;
    return [current, ...flattenMenuRows(children ?? [])];
  });
}

const menuTreeRows = computed(() => buildMenuRows(rawTree.value));
const menuRows = computed(() => flattenMenuRows(menuTreeRows.value));
const filteredRows = computed<MenuTreeRow[]>(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return menuTreeRows.value;
  }

  return menuRows.value.filter((item) =>
    [item.menuName, item.path, item.component, item.webPerms, item.apiPerms]
      .filter(Boolean)
      .some((field) => field!.toLowerCase().includes(value)),
  );
});
const parentOptions = computed(() =>
  menuRows.value.filter((item) => item.menuType !== 3),
);
const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    return '编辑菜单';
  }
  return addParentName.value ? `新增${addParentName.value}下级菜单` : '新增顶级菜单';
});

function getMenuTypeLabel(menuType: number) {
  return MENU_TYPE_OPTIONS.find((item) => item.value === menuType)?.label ?? '未知';
}

function resetForm() {
  Object.assign(formData, {
    apiPerms: '',
    cacheFlag: false,
    component: '',
    contextMenuId: null,
    disabledFlag: false,
    frameFlag: false,
    frameUrl: '',
    icon: '',
    menuName: '',
    menuType: 2,
    parentId: 0,
    path: '',
    permsType: 0,
    sort: 100,
    visibleFlag: true,
    webPerms: '',
  });
  formData.menuId = undefined;
  addParentName.value = '';
}

async function loadData() {
  loading.value = true;
  try {
    rawTree.value = await queryMenuTree(false);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  keyword.value = keyword.value.trim();
}

function handleReset() {
  keyword.value = '';
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openAddRootDialog() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openAddChildDialog(row: MenuTreeRow) {
  dialogMode.value = 'add';
  resetForm();
  formData.parentId = row.menuId;
  formData.contextMenuId = row.menuType === 2 ? row.menuId : null;
  addParentName.value = row.menuName;
  dialogVisible.value = true;
}

function openEditDialog(row: MenuTreeRow) {
  dialogMode.value = 'edit';
  addParentName.value = '';
  Object.assign(formData, {
    apiPerms: row.apiPerms || '',
    cacheFlag: row.cacheFlag,
    component: row.component || '',
    contextMenuId: row.contextMenuId ?? null,
    disabledFlag: row.disabledFlag,
    frameFlag: row.frameFlag,
    frameUrl: row.frameUrl || '',
    icon: row.icon || '',
    menuId: row.menuId,
    menuName: row.menuName,
    menuType: row.menuType,
    parentId: row.parentId ?? 0,
    path: row.path || '',
    permsType: row.permsType ?? 0,
    sort: row.sort ?? 0,
    visibleFlag: row.visibleFlag,
    webPerms: row.webPerms || '',
  });
  dialogVisible.value = true;
}

async function handleDelete(row: MenuTreeRow) {
  try {
    await ElMessageBox.confirm(`确定删除菜单“${row.menuName}”吗？`, '删除确认', {
      type: 'warning',
    });
    await batchDeleteMenus([row.menuId]);
    ElMessage.success('删除成功');
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addMenu(formData as MenuAddForm);
    ElMessage.success('新增菜单成功');
  } else {
    await updateMenu(formData as MenuUpdateForm);
    ElMessage.success('更新菜单成功');
  }

  dialogVisible.value = false;
  await loadData();
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '菜单数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="menu-page">
      <ElCard v-show="showSearchBar" class="menu-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="menu-page__keyword-item" label="关键字">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入菜单、路由、组件或权限码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="menu-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddRootDialog">新增顶级菜单</ElButton>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="columns"
            :data="filteredRows"
            :default-expand-all="true"
            height="100%"
            :indent="24"
            :loading="loading"
            row-key="menuId"
            :tree-props="{ children: 'children' }"
          >
            <template #menuName="{ row }">
              <div
                class="menu-page__tree-name"
                :style="{ paddingLeft: keyword.trim() ? `${row.level * 24}px` : '0px' }"
              >
                <span class="menu-page__tree-label">{{ row.menuName }}</span>
                <ElTag effect="plain" size="small">{{ getMenuTypeLabel(row.menuType) }}</ElTag>
              </div>
            </template>

            <template #status="{ row }">
              <ElSpace :size="6">
                <ElTag :type="row.visibleFlag ? 'success' : 'info'" effect="plain" size="small">
                  {{ row.visibleFlag ? '显示' : '隐藏' }}
                </ElTag>
                <ElTag :type="row.disabledFlag ? 'danger' : 'success'" effect="plain" size="small">
                  {{ row.disabledFlag ? '禁用' : '启用' }}
                </ElTag>
              </ElSpace>
            </template>

            <template #actions="{ row }">
              <ElSpace class="menu-page__actions">
                <ElButton link size="small" type="primary" @click="openAddChildDialog(row)">
                  新增下级
                </ElButton>
                <ElButton link size="small" type="primary" @click="openEditDialog(row)">
                  编辑
                </ElButton>
                <ElButton link size="small" type="danger" @click="handleDelete(row)">
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog v-model="dialogVisible" :title="dialogTitle" width="680px" @closed="resetForm">
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <div class="menu-page__form-grid">
          <ElFormItem label="菜单名称" prop="menuName">
            <ElInput v-model="formData.menuName" placeholder="请输入菜单名称" />
          </ElFormItem>
          <ElFormItem label="菜单类型" prop="menuType">
            <ElSelect v-model="formData.menuType" placeholder="请选择菜单类型">
              <ElOption
                v-for="item in MENU_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="上级菜单" prop="parentId">
            <ElSelect v-model="formData.parentId" clearable filterable placeholder="请选择上级菜单">
              <ElOption :value="0" label="顶级菜单" />
              <ElOption
                v-for="item in parentOptions"
                :key="item.menuId"
                :disabled="dialogMode === 'edit' && item.menuId === formData.menuId"
                :label="item.menuName"
                :value="item.menuId"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="排序" prop="sort">
            <ElInputNumber v-model="formData.sort" :min="0" style="width: 100%" />
          </ElFormItem>
          <ElFormItem label="路由路径" prop="path">
            <ElInput v-model="formData.path" placeholder="例如 /system/menu" />
          </ElFormItem>
          <ElFormItem label="组件路径" prop="component">
            <ElInput v-model="formData.component" placeholder="例如 /system/menu/index" />
          </ElFormItem>
          <ElFormItem label="前端权限码" prop="webPerms">
            <ElInput v-model="formData.webPerms" placeholder="例如 system:menu:add" />
          </ElFormItem>
          <ElFormItem label="后端权限码" prop="apiPerms">
            <ElInput v-model="formData.apiPerms" placeholder="例如 system:menu:add" />
          </ElFormItem>
          <ElFormItem label="权限类型" prop="permsType">
            <ElSelect v-model="formData.permsType" placeholder="请选择权限类型">
              <ElOption
                v-for="item in PERMS_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="图标" prop="icon">
            <ElInput v-model="formData.icon" placeholder="例如 lucide:settings" />
          </ElFormItem>
          <ElFormItem label="外链地址" prop="frameUrl">
            <ElInput v-model="formData.frameUrl" placeholder="请输入外链地址" />
          </ElFormItem>
          <ElFormItem label="关联菜单ID" prop="contextMenuId">
            <ElInputNumber v-model="formData.contextMenuId" :min="0" style="width: 100%" />
          </ElFormItem>
          <ElFormItem label="是否外链" prop="frameFlag">
            <ElSwitch v-model="formData.frameFlag" />
          </ElFormItem>
          <ElFormItem label="是否缓存" prop="cacheFlag">
            <ElSwitch v-model="formData.cacheFlag" />
          </ElFormItem>
          <ElFormItem label="是否显示" prop="visibleFlag">
            <ElSwitch v-model="formData.visibleFlag" />
          </ElFormItem>
          <ElFormItem label="是否禁用" prop="disabledFlag">
            <ElSwitch v-model="formData.disabledFlag" />
          </ElFormItem>
        </div>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">保存</ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.menu-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.menu-page__search-card,
.menu-page__table-card {
  border-radius: 8px;
}

.menu-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.menu-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.menu-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.menu-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.menu-page :deep(.art-table-panel),
.menu-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.menu-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.menu-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.menu-page__keyword-item :deep(.el-form-item__content) {
  width: 320px;
}

.menu-page__tree-name {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.menu-page__tree-label {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.menu-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.menu-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.menu-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.menu-page__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (width <= 768px) {
  .menu-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .menu-page__form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
