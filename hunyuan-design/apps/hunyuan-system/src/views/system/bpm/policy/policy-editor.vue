<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';

import type {
  BpmPolicyBusinessValidationResult,
  BpmPolicyType,
  BpmPolicyVisualDraft,
} from '#/api/system/bpm/policy';

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElTag,
} from 'element-plus';

import {
  activateBpmPolicyVersion,
  activateHighRiskBpmPolicyVersion,
  createBpmPolicyVisualDraft,
  getBpmPolicyCatalogVersion,
  saveBpmPolicyVisualDraft,
  validateBpmPolicyVisualDraft,
} from '#/api/system/bpm/policy';

import BpmApprovalRuleEditor from './components/bpm-approval-rule-editor.vue';
import BpmCandidateRuleEditor from './components/bpm-candidate-rule-editor.vue';
import BpmRuleSimulationPanel from './components/bpm-rule-simulation-panel.vue';
import BpmRuleTypeSelector from './components/bpm-rule-type-selector.vue';
import BpmScopeBuilder from './components/bpm-scope-builder.vue';
import { createPolicyModel, toPolicyVisualSaveParams } from './policy-editor-model';

defineOptions({ name: 'SystemBpmPolicyEditor' });

const route = useRoute();
const router = useRouter();

const formRef = ref<FormInstance>();
const saving = ref(false);
const validation = ref<BpmPolicyBusinessValidationResult>();

const existing = computed(() => Boolean(route.query.policyKey));
const pageTitle = computed(() =>
  existing.value ? '编辑审批规则' : '新建审批规则',
);

const model = reactive<BpmPolicyVisualDraft>(
  createPolicyModel((route.query.type as BpmPolicyType) || 'CANDIDATE'),
);

const validationSectionIndex = computed(() =>
  model.type === 'CANDIDATE' ? 4 : 3,
);

const riskLabel = (risk?: string) =>
  ({ HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[risk || ''] || risk || '');

const riskTagType = (risk?: string) =>
  (risk === 'HIGH' ? 'danger' : risk === 'MEDIUM' ? 'warning' : 'success') as
  | 'danger'
  | 'success'
  | 'warning';

const rules: FormRules<BpmPolicyVisualDraft> = {
  policyKey: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  policyName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
};

function replaceModel(value: BpmPolicyVisualDraft) {
  Object.keys(model).forEach((key) => delete (model as Record<string, unknown>)[key]);
  Object.assign(model, value);
}

function changeType(type: BpmPolicyType) {
  if (!existing.value) {
    replaceModel(createPolicyModel(type));
    validation.value = undefined;
  }
}

watch(
  () => model.type,
  (type, previous) => {
    if (type !== previous) {
      changeType(type);
    }
  },
);

async function validateBasicForm() {
  if (!formRef.value) {
    return true;
  }

  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
}

async function validate() {
  const passed = await validateBasicForm();
  if (!passed) {
    return false;
  }

  validation.value = await validateBpmPolicyVisualDraft(
    toPolicyVisualSaveParams(model),
  );

  if (validation.value.valid) {
    ElMessage.success('规则校验通过');
  }

  return validation.value.valid;
}

async function save() {
  const passed = await validateBasicForm();
  if (!passed) {
    return;
  }

  saving.value = true;
  try {
    const detail = existing.value
      ? await saveBpmPolicyVisualDraft(toPolicyVisualSaveParams(model))
      : await createBpmPolicyVisualDraft(toPolicyVisualSaveParams(model));

    model.policyVersion = detail.reference.policyVersion;
    model.catalogRevision = detail.catalogRevision;

    if (detail.businessSummary || detail.findings?.length) {
      validation.value = {
        valid: detail.findings?.every((item) => item.severity !== 'ERROR') ?? true,
        calculatedRiskLevel: detail.calculatedRiskLevel === 'UNKNOWN'
          ? 'LOW'
          : detail.calculatedRiskLevel,
        businessSummary: detail.businessSummary,
        findings: detail.findings || [],
      };
    }

    ElMessage.success('草稿已保存');
    return detail;
  } finally {
    saving.value = false;
  }
}

async function validateAndActivate() {
  const valid = await validate();
  if (!valid || !validation.value) {
    return;
  }

  const detail = await save();
  if (!detail) {
    return;
  }

  const params = {
    ...detail.reference,
    catalogRevision: detail.catalogRevision,
  };

  if (validation.value.calculatedRiskLevel === 'HIGH') {
    const { value } = await ElMessageBox.prompt(
      '该规则为高风险，请填写启用理由。',
      '高风险启用确认',
      {
        inputValidator: (text) => Boolean(text?.trim()) || '请输入确认理由',
        type: 'warning',
      },
    );
    await activateHighRiskBpmPolicyVersion({
      ...params,
      confirmationReason: value,
    });
  } else {
    await ElMessageBox.confirm('启用后该版本只读，确认继续？', '启用规则');
    await activateBpmPolicyVersion(params);
  }

  ElMessage.success('规则已启用');
  router.back();
}

onMounted(async () => {
  if (!existing.value) {
    return;
  }

  const detail = await getBpmPolicyCatalogVersion({
    type: route.query.type as BpmPolicyType,
    policyKey: String(route.query.policyKey),
    policyVersion: Number(route.query.policyVersion),
  });

  if (detail.configuration) {
    replaceModel({
      ...detail.configuration,
      policyVersion: detail.reference.policyVersion,
      catalogRevision: detail.catalogRevision,
    });
  }

  if (detail.businessSummary) {
    validation.value = {
      valid: true,
      calculatedRiskLevel: detail.calculatedRiskLevel === 'UNKNOWN'
        ? 'LOW'
        : detail.calculatedRiskLevel,
      businessSummary: detail.businessSummary,
      findings: detail.findings || [],
    };
  }
});
</script>

<template>
  <Page
    auto-content-height
    content-class="!flex !flex-col !p-3 h-full min-h-0 overflow-hidden"
  >
    <ArtEditPage :title="pageTitle">
      <template #back>
        <ElButton text @click="router.back()">返回</ElButton>
      </template>

      <template #extra>
        <ElTag effect="light" round type="warning">
          {{ existing ? `草稿 v${model.policyVersion}` : '新建草稿' }}
        </ElTag>
      </template>

      <template #actions>
        <ElButton :loading="saving" type="primary" @click="save">
          保存草稿
        </ElButton>
        <ElButton @click="validate">校验规则</ElButton>
        <ElButton type="success" @click="validateAndActivate">
          校验并启用
        </ElButton>
      </template>

      <ElForm
        ref="formRef"
        class="policy-edit-form"
        :model="model"
        :rules="rules"
        label-position="top"
      >
        <ArtEditSection :index="1" title="基本信息">
          <ElFormItem class="art-edit-section__full" label="规则类型">
            <BpmRuleTypeSelector v-model="model.type" :disabled="existing" />
          </ElFormItem>

          <ElFormItem label="规则名称" prop="policyName">
            <ElInput
              v-model="model.policyName"
              maxlength="128"
              placeholder="例如：部门负责人审批"
            />
          </ElFormItem>

          <ElFormItem label="规则编码" prop="policyKey">
            <ElInput
              v-model="model.policyKey"
              :disabled="existing"
              maxlength="128"
              placeholder="例如：dept-manager-candidate"
            />
          </ElFormItem>

          <ElFormItem class="art-edit-section__full" label="使用说明">
            <ElInput
              v-model="model.description"
              maxlength="500"
              placeholder="说明适用场景、适用范围和例外约定"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection :index="2" title="业务配置">
          <BpmCandidateRuleEditor
            v-if="model.candidate"
            v-model="model.candidate"
          />
          <BpmApprovalRuleEditor
            v-if="model.approval"
            v-model="model.approval"
          />
          <template v-if="model.startVisibility">
            <BpmScopeBuilder
              v-model="model.startVisibility.startScope"
              label="谁可以发起"
            />
            <BpmScopeBuilder
              v-model="model.startVisibility.visibilityScope"
              label="谁可以查看"
            />
          </template>
        </ArtEditSection>

        <ArtEditSection
          v-if="model.type === 'CANDIDATE'"
          :index="3"
          title="规则模拟"
        >
          <BpmRuleSimulationPanel :draft="model" />
        </ArtEditSection>

        <ArtEditSection
          v-if="validation"
          :index="validationSectionIndex"
          title="校验结果"
        >
          <div class="art-edit-section__full validation-block">
            <div class="validation-header">
              <ElTag
                v-if="validation.calculatedRiskLevel"
                :type="riskTagType(validation.calculatedRiskLevel)"
                effect="light"
              >
                {{ riskLabel(validation.calculatedRiskLevel) }}
              </ElTag>
              <p v-if="validation.businessSummary" class="validation-summary">
                {{ validation.businessSummary }}
              </p>
            </div>

            <ul v-if="validation.findings.length" class="validation-findings">
              <li
                v-for="finding in validation.findings"
                :key="`${finding.code}-${finding.fieldPath}`"
              >
                <span>{{ finding.message }}</span>
                <span v-if="finding.suggestion" class="validation-hint">
                  {{ finding.suggestion }}
                </span>
              </li>
            </ul>
          </div>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.policy-edit-form {
  display: grid;
  gap: 12px;
}

.policy-edit-form :deep(.el-select),
.policy-edit-form :deep(.el-input-number) {
  width: 100%;
}

.validation-block {
  display: grid;
  gap: 12px;
  padding-bottom: 14px;
}

.validation-header {
  display: grid;
  gap: 8px;
}

.validation-summary {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}

.validation-findings {
  margin: 0;
  padding: 0;
  list-style: none;
}

.validation-findings li {
  display: grid;
  gap: 2px;
  margin-top: 6px;
  color: var(--el-color-danger);
  font-size: 13px;
  line-height: 1.5;
}

.validation-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
