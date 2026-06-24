<script setup lang="ts">
import type { SupportedLanguagesType } from '@hunyuan/locales';

import { SUPPORT_LANGUAGES } from '@hunyuan/constants';
import { Languages } from '@hunyuan/icons';
import { loadLocaleMessages } from '@hunyuan/locales';
import { preferences, updatePreferences } from '@hunyuan/preferences';

import { HunyuanDropdownRadioMenu, HunyuanIconButton } from '@hunyuan-core/shadcn-ui';

defineOptions({
  name: 'LanguageToggle',
});

async function handleUpdate(value: string | undefined) {
  if (!value) return;
  const locale = value as SupportedLanguagesType;
  updatePreferences({
    app: {
      locale,
    },
  });
  await loadLocaleMessages(locale);
}
</script>

<template>
  <div>
    <HunyuanDropdownRadioMenu
      :menus="SUPPORT_LANGUAGES"
      :model-value="preferences.app.locale"
      @update:model-value="handleUpdate"
    >
      <HunyuanIconButton class="hover:animate-[shrink_0.3s_ease-in-out]">
        <Languages class="size-4 text-foreground" />
      </HunyuanIconButton>
    </HunyuanDropdownRadioMenu>
  </div>
</template>
