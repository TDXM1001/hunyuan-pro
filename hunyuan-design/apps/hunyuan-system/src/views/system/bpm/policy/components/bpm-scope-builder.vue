<script setup lang="ts">
import type { PolicyScopeVisualDocument } from '#/api/system/bpm/policy';
import type { DictOption } from '#/api/system/dict';

import { onMounted, ref } from 'vue';

import { ElFormItem, ElMessage, ElOption, ElSelect } from 'element-plus';

import { queryDictOptionsByCode } from '#/api/system/dict';
import BpmIdentityPicker from '#/components/bpm/identity/bpm-identity-picker.vue';

const props = defineProps<{ label: string }>();
const model = defineModel<PolicyScopeVisualDocument>({ required: true });

const scopeTypeOptions = ref<DictOption[]>([]);

async function loadDictOptions() {
  try {
    scopeTypeOptions.value = await queryDictOptionsByCode('BPM_POLICY_SCOPE_TYPE');
  } catch (error: any) {
    ElMessage.error(error?.message || '策略范围类型字典加载失败');
  }
}

onMounted(() => {
  void loadDictOptions();
});
</script>

<template>
  <div class="scope-builder">
    <ElFormItem :label="props.label">
      <ElSelect
        v-model="model.type"
        no-data-text="请先在字典中心维护 BPM_POLICY_SCOPE_TYPE"
        placeholder="请选择范围类型"
      >
        <ElOption
          v-for="item in scopeTypeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
    </ElFormItem>

    <ElFormItem v-if="model.type !== 'ALL'" label="选择范围">
      <BpmIdentityPicker
        :kind="
          model.type === 'ROLE_IDS'
            ? 'ROLE'
            : model.type === 'DEPARTMENT_IDS'
              ? 'DEPARTMENT'
              : 'EMPLOYEE'
        "
        :model-value="model.identities?.[0]"
        @update:model-value="model.identities = $event ? [$event] : []"
      />
    </ElFormItem>
  </div>
</template>

<style scoped>
.scope-builder {
  display: contents;
}
</style>
