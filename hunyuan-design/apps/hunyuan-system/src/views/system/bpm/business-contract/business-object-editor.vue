<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';

import type { BpmBusinessObjectDetail, BpmBusinessObjectDraft } from '#/api/system/bpm';
import type { DictOption } from '#/api/system/dict';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ArtEditPage, ArtEditSection } from '@vben/art-hooks/edit';

import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import {
  activateBpmBusinessContract,
  createBpmBusinessObjectVisualDraft,
  getBpmBusinessObjectDetail,
  saveBpmBusinessObjectVisualDraft,
  validateBpmBusinessObjectVisualDraft,
} from '#/api/system/bpm';
import { queryDictOptionsByCode } from '#/api/system/dict';

import BpmApplicationPreview from './components/bpm-application-preview.vue';
import BpmAttachmentRuleEditor from './components/bpm-attachment-rule-editor.vue';
import BpmBusinessKeyRuleEditor from './components/bpm-business-key-rule-editor.vue';
import BpmChangePolicyEditor from './components/bpm-change-policy-editor.vue';
import BpmLineItemDesigner from './components/bpm-line-item-designer.vue';
import BpmSchemaFieldTable from './components/bpm-schema-field-table.vue';
import { createBusinessObjectModel } from './business-object-editor-model';

const BUSINESS_TYPE_DICT_CODE = 'BPM_BUSINESS_TYPE';

const route = useRoute();
const router = useRouter();

const formRef = ref<FormInstance>();
const saving = ref(false);
const findings = ref<BpmBusinessObjectDetail['findings']>([]);
const summary = ref('');
const businessTypeOptions = ref<DictOption[]>([]);

const model = reactive<BpmBusinessObjectDraft>(createBusinessObjectModel());

const existing = computed(() => Boolean(route.query.contractKey));

const pageTitle = computed(() => (
  existing.value ? '编辑业务对象配置' : '新建业务对象配置'
));

const businessTypeSelectOptions = computed(() => {
  const options = [...businessTypeOptions.value];
  const currentValue = model.businessType?.trim();

  if (
    currentValue
    && !options.some((item) => item.value === currentValue)
  ) {
    options.unshift({
      label: `${currentValue}（当前值）`,
      value: currentValue,
    });
  }

  return options;
});

const allKeys = computed(() => [
  ...model.fieldSchema,
  ...model.routingFacts,
  ...model.workingDataSchema,
  ...(model.lineItemSchema?.fields || []),
]
  .map((field) => field.key)
  .filter(Boolean));

const previewDetail = computed<BpmBusinessObjectDetail>(() => ({
  businessSummary: summary.value,
  catalogRevision: model.catalogRevision,
  configuration: model,
  contractKey: model.contractKey,
  contractVersion: model.contractVersion,
  description: model.description,
  findings: findings.value,
  lifecycleState: 'DRAFT',
  objectName: model.objectName,
  referenceCount: 0,
  schemaVersion: 2,
}));

const rules: FormRules<BpmBusinessObjectDraft> = {
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  contractKey: [{ required: true, message: '请输入业务对象编码', trigger: 'blur' }],
  objectName: [{ required: true, message: '请输入业务对象名称', trigger: 'blur' }],
};

function replaceModel(value: BpmBusinessObjectDraft) {
  Object.keys(model).forEach((key) => delete (model as Record<string, unknown>)[key]);
  Object.assign(model, JSON.parse(JSON.stringify(value)));
}

async function loadBusinessTypeOptions() {
  try {
    businessTypeOptions.value = await queryDictOptionsByCode(BUSINESS_TYPE_DICT_CODE);
  } catch (error: any) {
    ElMessage.error(error?.message || '业务类型字典加载失败');
  }
}

async function loadDetail() {
  if (!existing.value) {
    return;
  }

  const detail = await getBpmBusinessObjectDetail(
    String(route.query.contractKey),
    Number(route.query.contractVersion),
  );

  if (detail.configuration) {
    replaceModel({
      ...detail.configuration,
      catalogRevision: detail.catalogRevision,
      contractVersion: detail.contractVersion,
      schemaVersion: 2,
    });
  }

  summary.value = detail.businessSummary;
}

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

  const result = await validateBpmBusinessObjectVisualDraft(model);
  findings.value = result.findings;
  summary.value = result.businessSummary;

  if (result.valid) {
    ElMessage.success('业务对象校验通过');
  }

  return result.valid;
}

async function save() {
  saving.value = true;

  try {
    const detail = existing.value
      ? await saveBpmBusinessObjectVisualDraft(model)
      : await createBpmBusinessObjectVisualDraft(model);

    model.contractVersion = detail.contractVersion;
    model.catalogRevision = detail.catalogRevision;
    findings.value = detail.findings;
    summary.value = detail.businessSummary;

    ElMessage.success('草稿已保存');
    return detail;
  } finally {
    saving.value = false;
  }
}

async function validateAndActivate() {
  const valid = await validate();
  if (!valid) {
    return;
  }

  const detail = await save();

  await ElMessageBox.confirm(
    '启用后该版本将转为只读，确认继续吗？',
    '启用业务对象',
  );

  await activateBpmBusinessContract({
    catalogRevision: detail.catalogRevision,
    contractKey: detail.contractKey,
    contractVersion: detail.contractVersion,
  });

  ElMessage.success('业务对象已启用');
  router.back();
}

onMounted(async () => {
  await Promise.all([
    loadBusinessTypeOptions(),
    loadDetail(),
  ]);
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtEditPage :title="pageTitle">
      <template #back>
        <ElButton text @click="router.back()">返回</ElButton>
      </template>

      <template #extra>
        <ElTag effect="plain" type="warning">
          {{ existing ? `草稿 v${model.contractVersion}` : '新建草稿' }}
        </ElTag>
      </template>

      <template #actions>
        <ElButton :loading="saving" type="primary" @click="save">保存草稿</ElButton>
        <ElButton @click="validate">校验配置</ElButton>
        <ElButton type="success" @click="validateAndActivate">校验并启用</ElButton>
      </template>

      <ElForm
        ref="formRef"
        class="business-object-edit-form"
        :model="model"
        :rules="rules"
        label-position="top"
      >
        <ArtEditSection :index="1" title="基本信息">
          <ElFormItem label="业务对象名称" prop="objectName">
            <ElInput
              v-model="model.objectName"
              maxlength="128"
              placeholder="例如：通用申请单"
            />
          </ElFormItem>

          <ElFormItem label="业务对象编码" prop="contractKey">
            <ElInput
              v-model="model.contractKey"
              :disabled="existing"
              maxlength="128"
              placeholder="例如：generic-application"
            />
          </ElFormItem>

          <ElFormItem label="业务类型" prop="businessType">
            <ElSelect
              v-model="model.businessType"
              clearable
              filterable
              placeholder="请选择业务类型"
              no-data-text="请先在字典中心维护 BPM_BUSINESS_TYPE"
            >
              <ElOption
                v-for="item in businessTypeSelectOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
            <div class="field-hint">
              业务管理员只需要选择业务含义，编码来源于字典 {{ BUSINESS_TYPE_DICT_CODE }}，
              不再直接填写类似 GENERIC_APPLICATION 的枚举值。
            </div>
          </ElFormItem>

          <ElFormItem class="art-edit-section__full" label="使用说明">
            <ElInput
              v-model="model.description"
              maxlength="500"
              placeholder="说明这个业务对象适用于什么场景、由谁发起、希望解决什么问题"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection :index="2" title="业务编号">
          <BpmBusinessKeyRuleEditor v-model="model.businessKeyRule" />
        </ArtEditSection>

        <ArtEditSection :index="3" title="申请字段">
          <BpmSchemaFieldTable
            v-model="model.fieldSchema"
            :all-keys="allKeys"
            title="申请人填写的字段"
            zone="APPLICATION"
          />
        </ArtEditSection>

        <ArtEditSection :index="4" title="流程路由字段">
          <BpmSchemaFieldTable
            v-model="model.routingFacts"
            :all-keys="allKeys"
            title="用于决定审批人的字段"
            zone="ROUTING"
          />
        </ArtEditSection>

        <ArtEditSection :index="5" title="审批过程字段">
          <BpmSchemaFieldTable
            v-model="model.workingDataSchema"
            :all-keys="allKeys"
            title="审批过程中记录的字段"
            zone="WORKING"
          />
        </ArtEditSection>

        <ArtEditSection :index="6" title="明细行">
          <BpmLineItemDesigner
            v-model="model.lineItemSchema"
            :all-keys="allKeys"
          />
        </ArtEditSection>

        <ArtEditSection :index="7" title="附件要求">
          <BpmAttachmentRuleEditor v-model="model.attachmentRule" />
        </ArtEditSection>

        <ArtEditSection :index="8" title="审批修改规则">
          <BpmChangePolicyEditor
            v-model="model.dataChangeRule"
            :working-fields="model.workingDataSchema"
          />
        </ArtEditSection>

        <ArtEditSection :index="9" title="实际表单预览">
          <BpmApplicationPreview :detail="previewDetail" />
        </ArtEditSection>

        <ArtEditSection
          v-if="summary || findings.length"
          :index="10"
          title="校验结果"
        >
          <div v-if="summary" class="validation-summary">{{ summary }}</div>

          <div
            v-for="finding in findings"
            :key="`${finding.code}-${finding.fieldPath}`"
            class="finding"
          >
            <strong>{{ finding.message }}</strong>
            <span>{{ finding.suggestion }}</span>
          </div>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>
  </Page>
</template>

<style scoped>
.business-object-edit-form {
  display: grid;
  gap: 12px;
}

.business-object-edit-form :deep(.el-select),
.business-object-edit-form :deep(.el-input-number) {
  width: 100%;
}

.field-hint {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.validation-summary {
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
}

.finding {
  display: grid;
  gap: 4px;
  margin-top: 8px;
  padding: 10px;
  border-left: 3px solid var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.finding span {
  color: var(--el-text-color-secondary);
}
</style>
