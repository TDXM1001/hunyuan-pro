<script setup lang="ts">
import { computed, ref } from 'vue'

import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  CacheInvalidationStrategy,
  type ColumnOption,
  type TableError,
  useTable,
} from '@vben/art-hooks/table'
import {
  ArtActionGroup,
  type ArtActionItem,
  ArtSearchPanel,
  ArtStatusTag,
  type ArtStatusTagMap,
} from '@vben/art-hooks/common'
import { Page } from '@vben/common-ui'

import {
  ElAlert,
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSpace,
  ElTag,
} from 'element-plus'

interface User {
  id: number
  username: string
  email: string
  realName: string
  roles: string[]
  status: number
}

interface UserListParams {
  current: number
  size: number
  username?: string
  email?: string
}

// 这里使用本地模拟数据，方便聚焦 useTable 的交互与样式表现。
const mockUsers: User[] = Array.from({ length: 100 }, (_, index) => ({
  id: index + 1,
  username: `user${index + 1}`,
  email: `user${index + 1}@example.com`,
  realName: `用户${index + 1}`,
  roles: index % 3 === 0 ? ['admin', 'user'] : ['user'],
  status: index % 5 === 0 ? 0 : 1,
}))

// 模拟服务端分页查询，让页面结构与真实业务表格保持一致。
async function fetchUserList(params: UserListParams) {
  await new Promise((resolve) => setTimeout(resolve, 500))

  let filteredUsers = [...mockUsers]

  if (params.username) {
    filteredUsers = filteredUsers.filter((user) => user.username.includes(params.username!))
  }
  if (params.email) {
    filteredUsers = filteredUsers.filter((user) => user.email.includes(params.email!))
  }

  const start = (params.current - 1) * params.size
  const end = start + params.size

  return {
    current: params.current,
    records: filteredUsers.slice(start, end),
    size: params.size,
    total: filteredUsers.length,
  }
}

const columnsFactory = (): ColumnOption<User>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'id', label: 'ID', width: 80, align: 'center' },
  { prop: 'username', label: '用户名', minWidth: 140 },
  { prop: 'email', label: '邮箱', minWidth: 220, showOverflowTooltip: true },
  { prop: 'realName', label: '真实姓名', minWidth: 120 },
  { prop: 'roles', label: '角色', minWidth: 140, useSlot: true },
  { prop: 'status', label: '状态', width: 110, align: 'center', useSlot: true },
  { prop: 'operation', label: '操作', width: 220, fixed: 'right', useSlot: true },
]

const userStatusMap: ArtStatusTagMap = {
  0: { text: '禁用', type: 'danger' },
  1: { text: '启用', type: 'success' },
}

const tableStandards = [
  {
    title: '页面结构',
    description: '统一由概览区、检索区、表格区组成，信息分层清晰，方便业务页面直接套用。',
  },
  {
    title: '列配置',
    description: '默认包含序号列、关键字段列、状态列、操作列；长文本字段开启溢出提示，操作列固定在右侧。',
  },
  {
    title: '交互约定',
    description: '搜索项支持重置与防抖刷新，表头统一使用刷新、列设置、尺寸、全屏等通用能力。',
  },
  {
    title: '刷新策略',
    description: '新增回第一页，更新保持当前页，删除智能回退页码，避免页面状态与用户预期不一致。',
  },
]

const {
  data,
  loading,
  pagination,
  searchParams,
  cacheInfo,
  columns,
  columnChecks,
  handleCurrentChange,
  handleSizeChange,
  getData,
  getDataDebounced,
  resetSearchParams,
  refreshData,
  refreshCreate,
  refreshUpdate,
  refreshRemove,
  clearCache,
  clearData,
} = useTable({
  core: {
    apiFn: fetchUserList,
    columnsFactory,
    immediate: true,
  },
  performance: {
    enableCache: true,
    cacheTime: 5 * 60 * 1000,
    debounceTime: 300,
  },
  hooks: {
    onCacheHit: () => {
      ElMessage.success('数据从缓存加载')
    },
    onError: (error: TableError) => {
      ElMessage.error(error.message)
    },
  },
  debug: {
    enableLog: false,
  },
})

const testResult = ref('')
const showSearchBar = ref(true)

const summaryItems = computed(() => [
  { label: '数据总量', value: `${pagination.total} 条`, type: 'success' as const },
  { label: '缓存条目', value: `${cacheInfo.value.total} 条`, type: 'info' as const },
  { label: '缓存命中率', value: cacheInfo.value.hitRate, type: undefined },
])

function handleSearchInput() {
  // 输入联动使用防抖刷新，保留参考项目里更轻快的列表检索体验。
  getDataDebounced()
}

function handleSearch() {
  getData()
}

function handleRefresh() {
  refreshData()
}

async function handleReset() {
  await resetSearchParams()
  ElMessage.success('搜索条件已重置')
}

async function handleEdit(row: User) {
  ElMessage.info(`编辑用户: ${row.username}`)
  await new Promise((resolve) => setTimeout(resolve, 300))
  await refreshUpdate()
  ElMessage.success('更新成功')
}

async function handleDelete() {
  await new Promise((resolve) => setTimeout(resolve, 300))
  await refreshRemove()
  ElMessage.success('删除成功')
}

function getUserActions(row: User): ArtActionItem[] {
  return [
    {
      key: 'edit',
      label: '编辑',
      onClick: () => handleEdit(row),
      type: 'primary',
    },
    {
      confirm: {
        message: `确定删除用户 ${row.username} 吗？`,
        title: '警告',
        type: 'warning',
      },
      key: 'delete',
      label: '删除',
      onClick: handleDelete,
      type: 'danger',
    },
  ]
}

async function testRefreshCreate() {
  testResult.value = '执行新增刷新：回到第一页并清空分页缓存。'
  await refreshCreate()
  testResult.value = '新增刷新完成，当前页码已重置为 1。'
}

async function testRefreshUpdate() {
  testResult.value = '执行更新刷新：保持当前页，只清理当前查询缓存。'
  await refreshUpdate()
  testResult.value = '更新刷新完成，当前页码保持不变。'
}

async function testRefreshRemove() {
  testResult.value = '执行删除刷新：智能回退页码，避免落在空页。'
  await refreshRemove()
  testResult.value = '删除刷新完成，页码已根据剩余数据自动调整。'
}

function testClearCache() {
  clearCache(CacheInvalidationStrategy.CLEAR_ALL, '手动测试')
  testResult.value = '所有缓存已清空。'
  ElMessage.success('缓存已清空')
}

function testClearData() {
  clearData()
  testResult.value = '表格数据已清空。'
  ElMessage.success('数据已清空')
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value
}
</script>

<template>
  <Page
    title="通用表格规范示例"
    description="这是一页可复用的 useTable 标准样板，用来统一通用表格的布局、列配置与刷新交互。"
  >
    <div class="table-test-page">
      <ElCard class="overview-card" shadow="never">
        <div class="overview-header">
          <div>
            <div class="overview-title">表格状态概览</div>
            <div class="overview-description">
              这一页用于验证列表检索、缓存命中、列配置与刷新策略是否正常协作。
            </div>
          </div>
          <div class="overview-tags">
            <ElTag
              v-for="item in summaryItems"
              :key="item.label"
              :type="item.type"
              effect="light"
              round
            >
              {{ item.label }}：{{ item.value }}
            </ElTag>
          </div>
        </div>
      </ElCard>

      <ElCard class="standards-card" shadow="never">
        <div class="table-panel-header">
          <div>
            <div class="table-panel-title">通用规范</div>
            <div class="table-panel-description">
              后续业务表格优先复用这一套结构，只替换查询项、列定义、插槽内容与接口实现。
            </div>
          </div>
        </div>

        <div class="standards-grid">
          <div v-for="item in tableStandards" :key="item.title" class="standard-item">
            <div class="standard-title">{{ item.title }}</div>
            <div class="standard-description">{{ item.description }}</div>
          </div>
        </div>
      </ElCard>

      <ArtSearchPanel
        v-show="showSearchBar"
        :loading="loading"
        @refresh="handleRefresh"
        @reset="handleReset"
        @search="handleSearch"
      >
        <ElFormItem label="用户名">
          <ElInput
            v-model="searchParams.username"
            placeholder="输入用户名搜索"
            clearable
            @input="handleSearchInput"
          />
        </ElFormItem>
        <ElFormItem label="邮箱">
          <ElInput
            v-model="searchParams.email"
            placeholder="输入邮箱搜索"
            clearable
            @input="handleSearchInput"
          />
        </ElFormItem>
      </ArtSearchPanel>

      <ElCard class="table-panel" shadow="never">
        <template #header>
          <div class="table-panel-header">
            <div>
              <div class="table-panel-title">用户列表示例</div>
              <div class="table-panel-description">
                公共表格样式已经下沉到 ArtTable 组件，这里只保留业务层的查询条件、列定义与插槽内容。
              </div>
            </div>
          </div>
        </template>

        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            :show-search-bar="showSearchBar"
            layout="search,refresh,size,fullscreen,columns,settings"
            @refresh="refreshData"
            @search="handleToggleSearchBar"
          />

          <ArtTable
            :columns="columns"
            :data="data"
            :loading="loading"
            :pagination="pagination"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #roles="{ row }">
              <ElSpace wrap size="small">
                <ElTag v-for="role in row.roles" :key="role" size="small" effect="light">
                  {{ role }}
                </ElTag>
              </ElSpace>
            </template>

            <template #status="{ row }">
              <ArtStatusTag :value="row.status" :map="userStatusMap" />
            </template>

            <template #operation="{ row }">
              <ArtActionGroup :actions="getUserActions(row)" />
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <ElCard class="test-card" shadow="never">
        <template #header>
          <div class="table-panel-header">
            <div>
              <div class="table-panel-title">刷新策略验证</div>
              <div class="table-panel-description">
                这里保留测试操作，方便验证 useTable 对新增、更新、删除与缓存失效的响应。
              </div>
            </div>
          </div>
        </template>

        <ElSpace wrap>
          <ElButton @click="testRefreshCreate">测试新增刷新</ElButton>
          <ElButton @click="testRefreshUpdate">测试更新刷新</ElButton>
          <ElButton @click="testRefreshRemove">测试删除刷新</ElButton>
          <ElButton @click="testClearCache">清空缓存</ElButton>
          <ElButton @click="testClearData">清空数据</ElButton>
        </ElSpace>

        <div v-if="testResult" class="test-result">
          <ElAlert :title="testResult" type="success" :closable="false" show-icon />
        </div>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.table-test-page {
  display: grid;
  gap: 16px;
}

.overview-card,
.standards-card,
.table-panel,
.test-card {
  border: 1px solid var(--el-border-color-lighter);
}

.overview-header,
.table-panel-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
}

.overview-title,
.table-panel-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  color: var(--el-text-color-primary);
}

.overview-description,
.table-panel-description {
  margin-top: 4px;
  font-size: 13px;
  line-height: 22px;
  color: var(--el-text-color-secondary);
}

.overview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.standards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.standard-item {
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.standard-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.standard-description {
  margin-top: 8px;
  font-size: 13px;
  line-height: 22px;
  color: var(--el-text-color-secondary);
}

.table-panel :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  padding-top: 18px;
}

.test-result {
  margin-top: 16px;
}

@media (width <= 768px) {
  .overview-header,
  .table-panel-header {
    flex-direction: column;
  }

  .overview-tags {
    justify-content: flex-start;
  }
}
</style>
