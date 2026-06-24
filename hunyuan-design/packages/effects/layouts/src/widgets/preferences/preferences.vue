<script lang="ts" setup>
import { computed } from 'vue';

import { Settings } from '@hunyuan/icons';
import { $t, loadLocaleMessages } from '@hunyuan/locales';
import { preferences, updatePreferences } from '@hunyuan/preferences';
import { capitalizeFirstLetter } from '@hunyuan/utils';

import { useHunyuanDrawer } from '@hunyuan-core/popup-ui';
import { HunyuanButton } from '@hunyuan-core/shadcn-ui';

import PreferencesDrawer from './preferences-drawer.vue';

interface Props {
  /** 是否显示按钮 */
  showButton?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showButton: true,
});

const emit = defineEmits<{ clearPreferencesAndLogout: [] }>();

const [Drawer, drawerApi] = useHunyuanDrawer({
  connectedComponent: PreferencesDrawer,
});

// 暴露打开抽屉的方法
defineExpose({
  open: () => drawerApi.open(),
});

/**
 * preferences 转成 vue props
 * preferences.widget.fullscreen=>widgetFullscreen
 */
const attrs = computed(() => {
  const result: Record<string, any> = {};
  for (const [key, value] of Object.entries(preferences)) {
    for (const [subKey, subValue] of Object.entries(value)) {
      result[`${key}${capitalizeFirstLetter(subKey)}`] = subValue;
    }
  }
  return result;
});

/**
 * preferences 转成 vue listener
 * preferences.widget.fullscreen=>@update:widgetFullscreen
 */
const listen = computed(() => {
  const result: Record<string, any> = {};
  for (const [key, value] of Object.entries(preferences)) {
    if (typeof value === 'object') {
      for (const subKey of Object.keys(value)) {
        result[`update:${key}${capitalizeFirstLetter(subKey)}`] = (
          val: any,
        ) => {
          updatePreferences({ [key]: { [subKey]: val } });
          if (key === 'app' && subKey === 'locale') {
            loadLocaleMessages(val);
          }
        };
      }
    } else {
      result[key] = value;
    }
  }
  return result;
});
</script>
<template>
  <div>
    <Drawer
      v-bind="{ ...$attrs, ...attrs }"
      v-on="listen"
      @clear-preferences-and-logout="emit('clearPreferencesAndLogout')"
    />

    <!-- 触发打开抽屉的按钮(可覆盖) -->
    <slot>
      <HunyuanButton
        v-if="props.showButton"
        :title="$t('preferences.title')"
        class="flex-col-center size-10 cursor-pointer rounded-l-lg rounded-r-none border-none bg-primary"
        @click="() => drawerApi.open()"
      >
        <Settings class="size-5" />
      </HunyuanButton>
    </slot>
  </div>
</template>
