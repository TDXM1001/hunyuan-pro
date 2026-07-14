<script setup lang="ts">
import type { BpmBusinessObjectField } from '#/api/system/bpm';
import { ref } from 'vue';
import { ArrowDown, ArrowUp, Delete, Edit, Plus } from '@element-plus/icons-vue';
import { ElButton, ElTable, ElTableColumn, ElTooltip } from 'element-plus';
import { createBusinessObjectField, moveBusinessObjectField } from '../business-object-editor-model';
import BpmSchemaFieldEditor from './bpm-schema-field-editor.vue';

defineProps<{ allKeys: string[]; title: string; zone: 'APPLICATION' | 'LINE_ITEM' | 'ROUTING' | 'WORKING' }>();
const model = defineModel<BpmBusinessObjectField[]>({ required: true });
const open = ref(false);
const editing = ref(-1);
function edit(index = -1) { editing.value = index; open.value = true; }
function save(field: BpmBusinessObjectField) { if (editing.value < 0) model.value.push(field); else model.value.splice(editing.value, 1, field); }
</script>

<template>
  <section class="field-block">
    <header><strong>{{ title }}</strong><ElButton :icon="Plus" type="primary" plain @click="edit()">新增字段</ElButton></header>
    <ElTable :data="model" empty-text="暂无字段">
      <ElTableColumn label="名称" min-width="130" prop="label" />
      <ElTableColumn label="类型" width="110" prop="type" />
      <ElTableColumn label="必填" width="70"><template #default="{ row }">{{ row.required ? '是' : '否' }}</template></ElTableColumn>
      <ElTableColumn fixed="right" label="操作" width="160" align="center"><template #default="{ $index }"><ElTooltip content="上移"><ElButton :disabled="$index === 0" :icon="ArrowUp" link @click="moveBusinessObjectField(model, $index, -1)" /></ElTooltip><ElTooltip content="下移"><ElButton :disabled="$index === model.length - 1" :icon="ArrowDown" link @click="moveBusinessObjectField(model, $index, 1)" /></ElTooltip><ElTooltip content="编辑"><ElButton :icon="Edit" link type="primary" @click="edit($index)" /></ElTooltip><ElTooltip content="删除"><ElButton :icon="Delete" link type="danger" @click="model.splice($index, 1)" /></ElTooltip></template></ElTableColumn>
    </ElTable>
    <BpmSchemaFieldEditor :existing-keys="allKeys.filter((key) => key !== model[editing]?.key)" :model-value="editing >= 0 ? model[editing] : createBusinessObjectField()" :open="open" :zone="zone" @close="open = false" @save="save" />
  </section>
</template>

<style scoped>.field-block { width: 100%; min-width: 0; } header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }</style>
