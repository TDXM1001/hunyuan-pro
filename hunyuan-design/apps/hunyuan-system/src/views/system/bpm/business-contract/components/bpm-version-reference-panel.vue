<script setup lang="ts">
import type { BpmBusinessObjectReference } from '#/api/system/bpm';

import { ElEmpty, ElTag } from 'element-plus';

defineProps<{ references: BpmBusinessObjectReference[] }>();

function publishedVersionLabel(item: BpmBusinessObjectReference) {
  const version = item.definitionVersion ?? item.graphDefinitionVersionId;
  return version == null ? '已发布' : `已发布 v${version}`;
}
</script>

<template>
  <div v-if="references.length" class="references">
    <ElTag
      v-for="item in references"
      :key="`${item.referenceSource}-${item.graphDefinitionVersionId || item.draftId}`"
      effect="plain"
    >
      {{ item.processName }}
      {{ item.referenceSource === 'DRAFT' ? '· Graph 草稿' : `· ${publishedVersionLabel(item)}` }}
    </ElTag>
  </div>
  <ElEmpty v-else description="暂无 Graph 引用" />
</template>

<style scoped>
.references {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
