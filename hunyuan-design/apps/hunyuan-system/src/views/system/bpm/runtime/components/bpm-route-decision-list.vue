<script setup lang="ts">
import type { BpmRouteDecisionRecord } from '#/api/system/bpm/runtime';
import { ElEmpty, ElTable, ElTableColumn, ElTag } from 'element-plus';
defineProps<{ decisions: BpmRouteDecisionRecord[] }>();
</script>

<template>
  <ElTable v-if="decisions.length" :data="decisions" border size="small">
    <ElTableColumn label="路由节点" min-width="130" prop="routeNodeKey" />
    <ElTableColumn label="命中分支" min-width="150">
      <template #default="{ row }"><ElTag v-for="key in row.matchedBranchKeys" :key="key" effect="plain" size="small">{{ key }}</ElTag></template>
    </ElTableColumn>
    <ElTableColumn label="表单版本" width="90" prop="inputFormDataVersion" />
    <ElTableColumn label="时间" min-width="150" prop="evaluatedAt" />
    <ElTableColumn label="原因摘要" min-width="220" prop="reasonSnapshotJson" show-overflow-tooltip />
  </ElTable>
  <ElEmpty v-else description="暂无路由记录" />
</template>
