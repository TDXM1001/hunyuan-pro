<script setup lang="ts">
import type { OperateLogRecord } from '#/api/system/operate-log';
import type { DetailSection } from '@vben/art-hooks/detail';

import { computed, ref, watch } from 'vue';

import { ArtDetail } from '@vben/art-hooks/detail';
import { useVbenDrawer } from '@vben/common-ui';

import {
  ElCard,
  ElSkeleton,
  ElSpace,
  ElTag,
} from 'element-plus';

import { queryOperateLogDetail } from '#/api/system/operate-log';

defineOptions({ name: 'SystemNetworkSecurityOperateLogDetailDrawer' });

const props = defineProps<{
  operateLog?: OperateLogRecord;
}>();

const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));

const loading = ref(false);
const detailData = ref<OperateLogRecord>();
const lastLoadedOperateLogId = ref<null | number>(null);

const drawerTitle = computed(() =>
  props.operateLog?.operateUserName
    ? `${props.operateLog.operateUserName} - 操作详情`
    : '操作日志详情',
);

const sections = computed<DetailSection<OperateLogRecord>[]>(() => [
  {
    key: 'basic',
    title: '基础信息',
    items: [
      { label: '操作人', prop: 'operateUserName' },
      { label: '模块', prop: 'module' },
      { label: '请求方法', prop: 'method' },
      { label: '请求结果', prop: 'successFlag', useSlot: true },
      { label: '客户端 IP', prop: 'ip' },
      { label: 'IP 地区', prop: 'ipRegion' },
      { label: 'User Agent', prop: 'userAgent', span: 2 },
      { label: '记录时间', prop: 'createTime' },
      { label: '更新时间', prop: 'updateTime' },
    ],
  },
  {
    key: 'request',
    title: '请求与返回',
    items: [
      { label: '请求地址', prop: 'url', span: 3 },
      { label: '操作内容', prop: 'content', span: 3 },
      { label: '失败原因', prop: 'failReason', span: 3 },
      { label: '请求参数', prop: 'param', span: 3 },
      { label: '返回结果', prop: 'response', span: 3 },
    ],
  },
]);

async function loadDetail() {
  if (!props.operateLog?.operateLogId) {
    detailData.value = undefined;
    return;
  }

  loading.value = true;
  try {
    detailData.value = await queryOperateLogDetail(props.operateLog.operateLogId);
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.operateLog?.operateLogId, drawerOpen.value] as const,
  ([operateLogId, isOpen]) => {
    if (!isOpen) {
      return;
    }

    if (!operateLogId) {
      detailData.value = undefined;
      return;
    }

    if (lastLoadedOperateLogId.value !== operateLogId) {
      lastLoadedOperateLogId.value = operateLogId;
      detailData.value = undefined;
    }

    void loadDetail();
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
    :title="drawerTitle"
  >
    <div class="operate-log-detail-drawer">
      <ElCard class="operate-log-detail-drawer__card" shadow="never">
        <ElSkeleton :loading="loading" animated>
          <template #template>
            <div class="operate-log-detail-drawer__skeleton">
              <ElSkeleton :rows="8" />
            </div>
          </template>

          <ArtDetail
            :data="detailData || {}"
            :sections="sections"
            :columns="3"
            :label-width="112"
          >
            <template #successFlag="{ value }">
              <ElTag
                :type="value ? 'success' : 'danger'"
                effect="plain"
                size="small"
              >
                {{ value ? '成功' : '失败' }}
              </ElTag>
            </template>
          </ArtDetail>
        </ElSkeleton>

        <template #footer>
          <ElSpace class="operate-log-detail-drawer__footer">
            <span>请求路径：{{ detailData?.url || '-' }}</span>
          </ElSpace>
        </template>
      </ElCard>
    </div>
  </Drawer>
</template>

<style scoped>
.operate-log-detail-drawer {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.operate-log-detail-drawer__card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.operate-log-detail-drawer__card :deep(.el-card__body) {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.operate-log-detail-drawer__skeleton {
  padding: 4px 0;
}

.operate-log-detail-drawer__footer {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
