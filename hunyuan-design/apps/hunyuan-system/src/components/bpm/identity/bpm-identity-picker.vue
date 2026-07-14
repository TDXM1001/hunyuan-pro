<script setup lang="ts">
import type { BpmIdentityOption, BpmIdentityReference } from '#/api/system/bpm/policy';
import { ref, watch } from 'vue';
import { ElOption, ElSelect } from 'element-plus';
import { queryBpmIdentityOptions } from '#/api/system/bpm/policy';
const props = defineProps<{ kind: BpmIdentityReference['kind']; modelValue?: BpmIdentityReference; disabled?: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value?: BpmIdentityReference] }>();
const options = ref<BpmIdentityOption[]>([]); const loading = ref(false);
async function search(keyword = '') { loading.value = true; try { options.value = (await queryBpmIdentityOptions({ kind: props.kind, keyword })).items; await restore(props.modelValue?.stableId); } finally { loading.value = false; } }
async function restore(stableId?: number) { if (!stableId || options.value.some((item)=>item.stableId===stableId)) return; const result=await queryBpmIdentityOptions({kind:props.kind,stableId}); if(result.items[0]) options.value=[result.items[0],...options.value]; }
function change(stableId?: number) { const option = options.value.find((item) => item.stableId === stableId); emit('update:modelValue', option ? { kind: props.kind, stableId: option.stableId, displayName: option.displayName } : undefined); }
watch(() => props.kind, () => void search(), { immediate: true });
watch(() => props.modelValue?.stableId, (stableId) => void restore(stableId), { immediate: true });
</script>
<template><ElSelect :disabled="disabled" :loading="loading" :model-value="modelValue?.stableId" filterable remote clearable :remote-method="search" @change="change"><ElOption v-for="option in options" :key="option.stableId" :disabled="option.disabled" :label="option.displayName" :value="option.stableId" /></ElSelect></template>
