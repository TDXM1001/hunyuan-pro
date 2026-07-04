<script setup lang="ts">
import { ArtStatusTag } from '@vben/art-hooks/common';

import { computed } from 'vue';
import { useRoute } from 'vue-router';

defineOptions({ name: 'SystemModuleBridge' });

interface BackendMenuMeta {
  backendComponent?: string;
  backendFrameUrl?: string;
  backendPath?: string;
  menuId?: number | null;
  menuType?: null | number;
  parentId?: number | null;
  title?: string;
}

const route = useRoute();

const backendMenu = computed<BackendMenuMeta>(() => {
  return (route.meta.backendMenu as BackendMenuMeta | undefined) ?? {};
});

const externalLink = computed(() => {
  const frameUrl = backendMenu.value.backendFrameUrl ?? '';
  const component = backendMenu.value.backendComponent ?? '';
  return frameUrl || (/^https?:\/\//i.test(component) ? component : '');
});

const moduleStatus = computed(() => {
  return externalLink.value ? 'external' : 'bridged';
});
</script>

<template>
  <div class="module-bridge-page">
    <section class="module-bridge-hero">
      <div class="module-bridge-copy">
        <p class="module-bridge-eyebrow">Hunyuan System</p>
        <h1>{{ route.meta.title || '模块桥接页' }}</h1>
        <p class="module-bridge-desc">
          该菜单已经接入真实登录、权限和菜单链路，但页面主体仍在迁移中。
          现在先通过统一桥接页承接，保证模块可见、可导航、可逐步替换。
        </p>
      </div>
      <ArtStatusTag
        :label="externalLink ? '外链模块' : '迁移中'"
        :type="externalLink ? 'primary' : 'warning'"
        :value="moduleStatus"
      />
    </section>

    <section class="module-bridge-panel">
      <div class="module-bridge-grid">
        <article class="module-card">
          <h2>后端菜单信息</h2>
          <dl>
            <div>
              <dt>菜单名称</dt>
              <dd>{{ backendMenu.title || route.meta.title || '-' }}</dd>
            </div>
            <div>
              <dt>菜单ID</dt>
              <dd>{{ backendMenu.menuId ?? '-' }}</dd>
            </div>
            <div>
              <dt>父级ID</dt>
              <dd>{{ backendMenu.parentId ?? '-' }}</dd>
            </div>
            <div>
              <dt>路由路径</dt>
              <dd>{{ backendMenu.backendPath || route.path }}</dd>
            </div>
          </dl>
        </article>

        <article class="module-card">
          <h2>迁移目标</h2>
          <dl>
            <div>
              <dt>后端组件</dt>
              <dd>{{ backendMenu.backendComponent || '-' }}</dd>
            </div>
            <div>
              <dt>外链地址</dt>
              <dd>{{ backendMenu.backendFrameUrl || externalLink || '-' }}</dd>
            </div>
            <div>
              <dt>当前状态</dt>
              <dd>{{ externalLink ? '可转外链/iframe' : '待页面迁移' }}</dd>
            </div>
            <div>
              <dt>建议动作</dt>
              <dd>
                {{
                  externalLink
                    ? '优先确认是否直接以 iframe 承接'
                    : '按模块批量补齐 views 并替换桥接页'
                }}
              </dd>
            </div>
          </dl>
        </article>
      </div>

      <article class="module-card module-card--full">
        <h2>推进建议</h2>
        <ol class="module-bridge-steps">
          <li>先保留当前菜单、权限和登录链路不动。</li>
          <li>以一个业务分组为单位补齐页面，比如“组织架构”或“系统设置”。</li>
          <li>页面落地后，只需让组件路径命中本地 `views`，桥接页会自动退出。</li>
        </ol>
      </article>
    </section>
  </div>
</template>

<style scoped>
.module-bridge-page {
  padding: 24px;
}

.module-bridge-hero,
.module-card {
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 1));
  border: 1px solid #dbe4ee;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.module-bridge-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
}

.module-bridge-copy {
  max-width: 760px;
}

.module-bridge-eyebrow {
  margin: 0 0 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.2;
}

.module-bridge-desc {
  margin: 14px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.module-bridge-panel {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}

.module-bridge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.module-card {
  padding: 22px 24px;
}

.module-card h2 {
  margin: 0 0 16px;
  color: #111827;
  font-size: 16px;
}

dl {
  display: grid;
  gap: 12px;
  margin: 0;
}

dl div {
  display: grid;
  gap: 4px;
}

dt {
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

dd {
  margin: 0;
  color: #111827;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.module-card--full {
  grid-column: 1 / -1;
}

.module-bridge-steps {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
}

@media (max-width: 800px) {
  .module-bridge-page {
    padding: 16px;
  }

  .module-bridge-hero {
    flex-direction: column;
    padding: 22px;
  }

  .module-bridge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
