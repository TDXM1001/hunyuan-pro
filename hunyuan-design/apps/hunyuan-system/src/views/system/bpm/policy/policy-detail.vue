<script setup lang="ts">
import type {
  BpmDefinitionReference,
  BpmPolicyBusinessDetail,
  BpmPolicyType,
} from '#/api/system/bpm/policy';
import type { DictOption } from '#/api/system/dict';
import type { DetailSection } from '@vben/art-hooks/detail';

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAccess } from '@vben/access';
import { ArtDetail, ArtDetailPage } from '@vben/art-hooks/detail';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElEmpty,
  ElMessage,
  ElSkeleton,
  ElTag,
} from 'element-plus';

import {
  exportBpmPolicyTechnicalDetail,
  getBpmPolicyCatalogVersion,
  queryBpmPolicyReferences,
} from '#/api/system/bpm/policy';
import { queryDictOptionsByCode } from '#/api/system/dict';

import BpmTechnicalPolicyPanel from './components/bpm-technical-policy-panel.vue';

defineOptions({ name: 'SystemBpmPolicyDetail' });

const route = useRoute();
const router = useRouter();
const { hasAccessByCodes, hasAccessByRoles } = useAccess();

const loading = ref(true);
const detail = ref<BpmPolicyBusinessDetail>();
const references = ref<BpmDefinitionReference[]>([]);
const typeOptions = ref<DictOption[]>([]);
const lifecycleOptions = ref<DictOption[]>([]);

const canViewTechnical = computed(
  () =>
    hasAccessByRoles(['admin'])
    || hasAccessByCodes(['bpm:policy-catalog:technical']),
);

const typeLabelMap = computed(() =>
  Object.fromEntries(typeOptions.value.map((item) => [item.value, item.label])),
);

const stateLabelMap = computed(() =>
  Object.fromEntries(
    lifecycleOptions.value.map((item) => [item.value, item.label]),
  ),
);

const typeLabel = (type?: BpmPolicyType) =>
  (type && typeLabelMap.value[type]) || type || '-';

const stateLabel = (state?: string) =>
  (state && stateLabelMap.value[state]) || state || '-';

const stateTagType = (state?: string) =>
  (state === 'ACTIVE' ? 'success' : state === 'DRAFT' ? 'warning' : 'info') as
  | 'info'
  | 'success'
  | 'warning';

const riskLabel = (risk?: string) =>
  ({ HIGH: '高', MEDIUM: '中', LOW: '低', UNKNOWN: '未知' }[risk || ''] || risk || '-');

const riskTagType = (risk?: string) =>
  (risk === 'HIGH' ? 'danger' : risk === 'MEDIUM' ? 'warning' : 'success') as
  | 'danger'
  | 'success'
  | 'warning';

const detailView = computed(() => {
  if (!detail.value) {
    return {};
  }

  return {
    ...detail.value,
    policyKey: detail.value.reference.policyKey,
    policyType: typeLabel(detail.value.reference.type),
    policyVersion: `v${detail.value.reference.policyVersion}`,
  };
});

const sections = computed<DetailSection<Record<string, any>>[]>(() => [
  {
    key: 'basic',
    title: '基本信息',
    items: [
      { label: '规则名称', prop: 'policyName' },
      { label: '规则编码', prop: 'policyKey' },
      { label: '规则类型', prop: 'policyType' },
      { label: '版本', prop: 'policyVersion' },
      { label: '状态', prop: 'lifecycleState', useSlot: true },
      { label: '风险', prop: 'calculatedRiskLevel', useSlot: true },
      { label: '使用说明', prop: 'description', span: 3 },
      { label: '业务摘要', prop: 'businessSummary', span: 3 },
    ],
  },
  {
    key: 'references',
    title: 'Graph 引用',
    items: [
      {
        label: '引用关系',
        prop: 'referenceCount',
        span: 3,
        slotName: 'references',
        useSlot: true,
      },
    ],
  },
]);

function publishedVersionLabel(item: BpmDefinitionReference) {
  return item.definitionVersion == null
    ? '已发布'
    : `已发布 v${item.definitionVersion}`;
}

async function loadDictOptions() {
  try {
    const [types, lifecycles] = await Promise.all([
      queryDictOptionsByCode('BPM_POLICY_TYPE'),
      queryDictOptionsByCode('BPM_POLICY_LIFECYCLE_STATE'),
    ]);
    typeOptions.value = types;
    lifecycleOptions.value = lifecycles;
  } catch (error: any) {
    ElMessage.error(error?.message || '审批规则字典加载失败');
  }
}

async function loadDetail() {
  loading.value = true;
  try {
    const reference = {
      type: route.query.type as BpmPolicyType,
      policyKey: String(route.query.policyKey),
      policyVersion: Number(route.query.policyVersion),
    };
    detail.value = await getBpmPolicyCatalogVersion(reference);
    references.value = await queryBpmPolicyReferences(detail.value.reference);
  } finally {
    loading.value = false;
  }
}

async function exportProtocol() {
  if (!detail.value) {
    return;
  }

  const content = (await exportBpmPolicyTechnicalDetail(
    detail.value.reference,
  )) as Blob;
  const url = URL.createObjectURL(content);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${detail.value.reference.policyKey}-v${detail.value.reference.policyVersion}.json`;
  link.click();
  URL.revokeObjectURL(url);
}

onMounted(() => {
  void Promise.all([loadDictOptions(), loadDetail()]);
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ArtDetailPage title="审批规则详情">
      <template #back>
        <ElButton text @click="router.back()">返回</ElButton>
      </template>

      <template v-if="detail" #extra>
        <ElTag :type="stateTagType(detail.lifecycleState)" effect="light">
          {{ stateLabel(detail.lifecycleState) }}
        </ElTag>
      </template>

      <template #actions>
        <ElButton v-if="canViewTechnical" @click="exportProtocol">
          导出协议
        </ElButton>
      </template>

      <ElSkeleton :loading="loading" animated>
        <template #template>
          <div class="detail-skeleton">
            <ElSkeleton :rows="8" />
          </div>
        </template>

        <template v-if="detail">
          <ArtDetail
            :data="detailView"
            :sections="sections"
            :columns="3"
            :label-width="112"
          >
            <template #lifecycleState="{ value }">
              <ElTag :type="stateTagType(value)" effect="light">
                {{ stateLabel(value) }}
              </ElTag>
            </template>

            <template #calculatedRiskLevel="{ value }">
              <ElTag :type="riskTagType(value)" effect="light">
                {{ riskLabel(value) }}
              </ElTag>
            </template>

            <template #references>
              <div v-if="references.length" class="references">
                <ElTag
                  v-for="item in references"
                  :key="`${item.referenceSource}-${item.graphDefinitionVersionId || item.draftId}`"
                  effect="plain"
                >
                  {{ item.processName }}
                  {{
                    item.referenceSource === 'DRAFT'
                      ? '· Graph 草稿'
                      : `· ${publishedVersionLabel(item)}`
                  }}
                </ElTag>
              </div>
              <ElEmpty v-else description="暂无 Graph 引用" />
            </template>
          </ArtDetail>

          <section v-if="canViewTechnical" class="technical">
            <BpmTechnicalPolicyPanel :reference="detail.reference" />
          </section>
        </template>

        <ElEmpty v-else-if="!loading" description="未找到审批规则" />
      </ElSkeleton>
    </ArtDetailPage>
  </Page>
</template>

<style scoped>
.detail-skeleton {
  padding: 8px 0;
}

.references {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.technical {
  margin-top: 12px;
}
</style>
