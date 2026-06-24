<script lang="ts" setup>
import type { DropdownMenuProps } from '@hunyuan-core/shadcn-ui';

import { LayoutGrid } from '@hunyuan-core/icons';
import { HunyuanDropdownMenu } from '@hunyuan-core/shadcn-ui';

defineProps<DropdownMenuProps>();
</script>

<template>
  <HunyuanDropdownMenu :menus="menus" :modal="false">
    <div
      class="flex-center h-full cursor-pointer border-l border-border px-2 text-lg font-semibold text-muted-foreground hover:bg-muted hover:text-foreground"
    >
      <LayoutGrid class="size-4" />
    </div>
  </HunyuanDropdownMenu>
</template>
