<script setup lang="ts">
import type { CandidatePolicyVisualDocument } from '#/api/system/bpm/policy';
import type { DictOption } from '#/api/system/dict';

import { computed, onMounted, ref } from 'vue';

import { ElFormItem, ElMessage, ElOption, ElSelect } from 'element-plus';

import { queryDictOptionsByCode } from '#/api/system/dict';
import BpmIdentityPicker from '#/components/bpm/identity/bpm-identity-picker.vue';

const VISUAL_RESOLVER_TYPES = new Set([
  'ROLE',
  'EMPLOYEE',
  'START_EMPLOYEE',
  'START_DEPARTMENT_MANAGER',
]);

const model = defineModel<CandidatePolicyVisualDocument>({ required: true });

const resolverOptions = ref<DictOption[]>([]);
const selfApprovalOptions = ref<DictOption[]>([]);
const emptyCandidateOptions = ref<DictOption[]>([]);

const visualResolverOptions = computed(() =>
  resolverOptions.value.filter((item) => VISUAL_RESOLVER_TYPES.has(item.value)),
);

async function loadDictOptions() {
  try {
    const [resolver, selfApproval, emptyCandidate] = await Promise.all([
      queryDictOptionsByCode('BPM_CANDIDATE_RESOLVER_TYPE'),
      queryDictOptionsByCode('BPM_SELF_APPROVAL_POLICY'),
      queryDictOptionsByCode('BPM_EMPTY_CANDIDATE_POLICY'),
    ]);
    resolverOptions.value = resolver;
    selfApprovalOptions.value = selfApproval;
    emptyCandidateOptions.value = emptyCandidate;
  } catch (error: any) {
    ElMessage.error(error?.message || '审批人规则字典加载失败');
  }
}

onMounted(() => {
  void loadDictOptions();
});
</script>

<template>
  <div class="candidate-rule-editor">
    <ElFormItem label="审批人来源">
      <ElSelect
        v-model="model.resolverType"
        no-data-text="请先在字典中心维护 BPM_CANDIDATE_RESOLVER_TYPE"
        placeholder="请选择审批人来源"
      >
        <ElOption
          v-for="item in visualResolverOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
    </ElFormItem>

    <ElFormItem
      v-if="['ROLE', 'EMPLOYEE'].includes(model.resolverType)"
      label="选择对象"
    >
      <BpmIdentityPicker
        v-model="model.identityReference"
        :kind="model.resolverType as 'ROLE' | 'EMPLOYEE'"
      />
    </ElFormItem>

    <ElFormItem label="发起人也是审批人">
      <ElSelect
        v-model="model.selfApprovalPolicy"
        no-data-text="请先在字典中心维护 BPM_SELF_APPROVAL_POLICY"
        placeholder="请选择自审处理方式"
      >
        <ElOption
          v-for="item in selfApprovalOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
    </ElFormItem>

    <ElFormItem label="找不到审批人">
      <ElSelect
        v-model="model.emptyCandidatePolicy"
        no-data-text="请先在字典中心维护 BPM_EMPTY_CANDIDATE_POLICY"
        placeholder="请选择找不到审批人时的处理方式"
      >
        <ElOption
          v-for="item in emptyCandidateOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
    </ElFormItem>
  </div>
</template>

<style scoped>
.candidate-rule-editor {
  display: contents;
}
</style>
