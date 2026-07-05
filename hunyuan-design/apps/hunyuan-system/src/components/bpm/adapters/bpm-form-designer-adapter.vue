<script setup lang="ts">
import type {
  BpmFormDesignerExpose,
  BpmFormDesignerSnapshot,
} from './types';

import FcDesigner from '@form-create/designer';
import { computed, nextTick, onMounted, ref } from 'vue';

defineOptions({ name: 'BpmFormDesignerAdapter' });

const emit = defineEmits<{
  change: [snapshot: BpmFormDesignerSnapshot];
  ready: [];
}>();

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    initialSnapshot?: Partial<BpmFormDesignerSnapshot>;
    readonly?: boolean;
  }>(),
  {
    disabled: false,
    initialSnapshot: () => ({}),
    readonly: false,
  },
);

const designerRef = ref<any>();
const dirty = ref(false);

const designerConfig = computed(() => ({
  autoActive: true,
  fieldReadonly: props.disabled || props.readonly,
  formOptions: {
    form: {
      labelWidth: '100px',
    },
  },
  hiddenDragBtn: false,
  hiddenDragMenu: false,
  showBaseForm: true,
  showConfig: true,
  showControl: true,
  showDevice: true,
  showEventForm: true,
  showFormConfig: true,
  showInputData: true,
  showPropsForm: true,
  showSaveBtn: false,
  showValidateForm: true,
  useTemplate: false,
}));

function safeParseJson<T>(jsonText: string | undefined, fallbackValue: T): T {
  if (!jsonText?.trim()) {
    return fallbackValue;
  }

  try {
    return JSON.parse(jsonText) as T;
  } catch {
    return fallbackValue;
  }
}

function getSnapshot(): BpmFormDesignerSnapshot {
  return {
    layoutJson: JSON.stringify(designerRef.value?.getOption?.() || {}),
    schemaJson: JSON.stringify(designerRef.value?.getRule?.() || []),
  };
}

async function load(snapshot: Partial<BpmFormDesignerSnapshot>) {
  await nextTick();

  const rules = safeParseJson(snapshot.schemaJson, []);
  const options = safeParseJson(snapshot.layoutJson, {});

  designerRef.value?.setRule?.(rules);
  designerRef.value?.setOption?.(options);
  dirty.value = false;
}

async function validate() {
  const snapshot = getSnapshot();
  const hasSchema = snapshot.schemaJson.trim() !== '[]';

  return {
    message: hasSchema ? undefined : '请先配置至少一个表单字段',
    ok: hasSchema,
  };
}

function resetDirty() {
  dirty.value = false;
}

function isDirty() {
  return dirty.value;
}

function handleDesignerChange() {
  dirty.value = true;
  emit('change', getSnapshot());
}

defineExpose<BpmFormDesignerExpose>({
  getSnapshot,
  isDirty,
  load,
  resetDirty,
  validate,
});

onMounted(async () => {
  await load(props.initialSnapshot);
  emit('ready');
});
</script>

<template>
  <div class="bpm-form-designer-adapter">
    <FcDesigner
      ref="designerRef"
      class="bpm-form-designer-adapter__designer"
      :config="designerConfig"
      @change="handleDesignerChange"
    />
  </div>
</template>

<style scoped>
.bpm-form-designer-adapter {
  height: 100%;
  min-height: 520px;
  overflow: hidden;
}

.bpm-form-designer-adapter__designer {
  height: 100%;
}
</style>
