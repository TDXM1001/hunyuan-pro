<script setup lang="ts">
import { ArtStatusTag } from '@vben/art-hooks/common';

defineOptions({ name: 'SystemHome' });
</script>

<template>
  <div class="system-home-page">
    <section class="system-home-header">
      <div>
        <p class="system-home-eyebrow">Hunyuan System</p>
        <h1>系统管理前端</h1>
        <p class="system-home-desc">
          完整应用壳已迁移，下一步接入真实登录、用户信息、菜单和权限。
        </p>
      </div>
      <ArtStatusTag label="应用壳" type="success" value="ready" />
    </section>
  </div>
</template>

<style scoped>
.system-home-page {
  padding: 24px;
}

.system-home-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.system-home-eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

h1 {
  margin: 0;
  color: #1f2937;
  font-size: 24px;
  line-height: 1.3;
}

.system-home-desc {
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 640px) {
  .system-home-page {
    padding: 16px;
  }

  .system-home-header {
    flex-direction: column;
  }
}
</style>

