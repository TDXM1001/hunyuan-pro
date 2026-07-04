<script setup lang="ts">
import SystemMenuManagement from './index.vue';

defineOptions({ name: 'SystemMenuList' });
</script>

<template>
  <!-- 兼容后端菜单组件路径 /system/menu/menu-list.vue，实际页面实现集中在 index.vue。 -->
  <SystemMenuManagement />
</template>
