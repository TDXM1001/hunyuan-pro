<script setup lang="ts">
import type { TreeProps } from '@hunyuan-core/shadcn-ui';

import { Inbox } from '@hunyuan/icons';
import { $t } from '@hunyuan/locales';

import { treePropsDefaults, HunyuanTree } from '@hunyuan-core/shadcn-ui';

const props = withDefaults(defineProps<TreeProps>(), treePropsDefaults());
</script>

<template>
  <HunyuanTree v-if="props.treeData?.length > 0" v-bind="props">
    <template v-for="(_, key) in $slots" :key="key" #[key]="slotProps">
      <slot :name="key" v-bind="slotProps"> </slot>
    </template>
  </HunyuanTree>
  <div
    v-else
    class="flex-col-center cursor-pointer rounded-lg border p-10 text-sm font-medium text-muted-foreground"
  >
    <Inbox class="size-10" />
    <div class="mt-1">{{ $t('common.noData') }}</div>
  </div>
</template>
