<script setup lang="ts">
import type { MenuRecordRaw } from '@hunyuan-core/typings';

import type { NormalMenuProps } from './normal-menu';

import { useNamespace } from '@hunyuan-core/composables';
import { HunyuanIcon } from '@hunyuan-core/shadcn-ui';

interface Props extends NormalMenuProps {}

defineOptions({
  name: 'NormalMenu',
});

const props = withDefaults(defineProps<Props>(), {
  activePath: '',
  collapse: false,
  menus: () => [],
  theme: 'dark',
});

const emit = defineEmits<{
  enter: [MenuRecordRaw];
  select: [MenuRecordRaw];
}>();

const { b, e, is } = useNamespace('normal-menu');

function menuIcon(menu: MenuRecordRaw) {
  return props.activePath === menu.path
    ? menu.activeIcon || menu.icon
    : menu.icon;
}
</script>

<template>
  <ul
    :class="[
      theme,
      b(),
      is('collapse', collapse),
      is(theme, true),
      is('rounded', rounded),
    ]"
    class="relative"
  >
    <template v-for="menu in menus" :key="menu.path">
      <li
        :class="[e('item'), is('active', activePath === menu.path)]"
        @click="() => emit('select', menu)"
        @mouseenter="() => emit('enter', menu)"
      >
        <HunyuanIcon :class="e('icon')" :icon="menuIcon(menu)" fallback />

        <span :class="e('name')" class="truncate"> {{ menu.name }}</span>
      </li>
    </template>
  </ul>
</template>
<style scoped>
@reference "@hunyuan/tailwind-config/theme";

.hunyuan-normal-menu {
  --menu-item-margin-y: 4px;
  --menu-item-margin-x: 0px;
  --menu-item-padding-y: 9px;
  --menu-item-padding-x: 0px;
  --menu-item-radius: 0px;

  height: calc(100% - 4px);
}

.hunyuan-normal-menu.is-rounded {
  --menu-item-radius: 6px;
  --menu-item-margin-x: 8px;
}

.hunyuan-normal-menu.is-dark .hunyuan-normal-menu__item {
  @apply text-foreground/80;
}

.hunyuan-normal-menu.is-dark .hunyuan-normal-menu__item:not(.is-active):hover {
  @apply text-foreground;
}

.hunyuan-normal-menu.is-dark
  .hunyuan-normal-menu__item.is-active
  .hunyuan-normal-menu__name,
.hunyuan-normal-menu.is-dark
  .hunyuan-normal-menu__item.is-active
  .hunyuan-normal-menu__icon {
  @apply text-foreground;
}

.hunyuan-normal-menu.is-collapse .hunyuan-normal-menu__name {
  width: 0;
  height: 0;
  margin-top: 0;
  overflow: hidden;
  opacity: 0;
}

.hunyuan-normal-menu.is-collapse .hunyuan-normal-menu__icon {
  font-size: calc(var(--font-size-base, 16px) * 1.25);
}

.hunyuan-normal-menu__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  /* max-width: 64px; */

  /* max-height: 64px; */
  padding: var(--menu-item-padding-y) var(--menu-item-padding-x);
  margin: var(--menu-item-margin-y) var(--menu-item-margin-x);
  color: hsl(var(--foreground) / 90%);
  cursor: pointer;
  border-radius: var(--menu-item-radius);
  transition:
    background 0.15s ease,
    padding 0.15s ease,
    border-color 0.15s ease;
}

.hunyuan-normal-menu__item.is-active {
  @apply bg-primary text-primary dark:bg-accent;
}

.hunyuan-normal-menu__item.is-active .hunyuan-normal-menu__name,
.hunyuan-normal-menu__item.is-active .hunyuan-normal-menu__icon {
  @apply text-primary-foreground font-semibold;
}

.hunyuan-normal-menu__item:not(.is-active):hover {
  @apply bg-heavy text-primary dark:bg-accent dark:text-foreground;
}

.hunyuan-normal-menu__item:hover .hunyuan-normal-menu__icon {
  transform: scale(1.2);
}

.hunyuan-normal-menu__icon {
  @apply max-h-5;

  font-size: calc(var(--font-size-base, 16px) * 1.25);
  transition: all 0.25s ease;
}

.hunyuan-normal-menu__name {
  @apply mt-2;

  width: 100%;
  margin-bottom: 0;
  font-size: calc(var(--font-size-base, 16px) * 0.75);
  font-weight: 400;
  text-align: center;
  transition: all 0.25s ease;
}
</style>
