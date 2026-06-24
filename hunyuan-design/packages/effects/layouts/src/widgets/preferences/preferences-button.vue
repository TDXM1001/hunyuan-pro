<script lang="ts" setup>
import { ref } from 'vue';

import { Settings } from '@hunyuan/icons';

import { HunyuanIconButton } from '@hunyuan-core/shadcn-ui';

import Preferences from './preferences.vue';

const emit = defineEmits<{ clearPreferencesAndLogout: [] }>();

function clearPreferencesAndLogout() {
  emit('clearPreferencesAndLogout');
}

const preferencesRef = ref<InstanceType<typeof Preferences> | null>(null);
</script>

<template>
  <Preferences
    ref="preferencesRef"
    @clear-preferences-and-logout="clearPreferencesAndLogout"
  >
    <HunyuanIconButton
      class="hover:animate-[shrink_0.3s_ease-in-out]"
      @click="preferencesRef?.open()"
    >
      <Settings class="size-4 text-foreground" />
    </HunyuanIconButton>
  </Preferences>
</template>
