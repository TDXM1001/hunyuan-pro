import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const apiFiles = [
  {
    label: 'category',
    needles: ['/bpm/category/query', '/bpm/category/add', '/bpm/category/update'],
    path: 'apps/hunyuan-system/src/api/system/bpm/category.ts',
  },
  {
    label: 'form',
    needles: [
      '/bpm/form/query',
      '/bpm/form/add',
      '/bpm/form/update',
      'buildEmptyBpmFormDesignerSnapshot',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/form.ts',
  },
  {
    label: 'model',
    needles: [
      '/bpm/model/query',
      '/bpm/designer/detail/',
      '/bpm/definition/publish',
      'buildEmptyBpmDesignerDraft',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/model.ts',
  },
  {
    label: 'definition',
    needles: [
      '/bpm/definition/query',
      '/bpm/definition/detail/',
      'validateBpmDefinitionForPublish',
      '/bpm/definition/validateForPublish/',
      'getBpmDefinitionPublishDiff',
      '/bpm/definition/publishDiff/',
      'saveBpmDefinitionStartScope',
      '/bpm/definition/startScope/save',
      'suspendBpmDefinitionStart',
      '/bpm/definition/suspendStart/',
      'enableBpmDefinitionStart',
      '/bpm/definition/enableStart/',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/definition.ts',
  },
  {
    label: 'runtime',
    needles: [
      '/bpm/instance/query',
      '/bpm/instance/detail/',
      '/bpm/instance/trace/',
      'getBpmAdminInstanceTrace',
      'BpmInstanceTraceRecord',
      'BpmNotificationRecordVO',
      'notificationRecords',
      '/bpm/task/detail/',
      '/app/bpm/startable',
      '/app/bpm/start-draft/',
      '/app/bpm/resubmit-draft/',
      '/app/bpm/instance/cancel',
      '/app/bpm/instance/resubmit',
      '/app/bpm/my-copy',
      '/app/bpm/copy/read/',
      '/app/bpm/task/approve',
      '/app/bpm/task/returnToInitiator',
      'copyEmployeeIds',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/runtime.ts',
  },
  {
    label: 'listener',
    needles: ['/bpm/listener/query', '/bpm/listener/channelOptions'],
    path: 'apps/hunyuan-system/src/api/system/bpm/listener.ts',
  },
  {
    label: 'sampleExpense',
    needles: [
      'createBpmSampleExpense',
      '/bpm/sample/expense/create',
      'startBpmSampleExpense',
      '/bpm/sample/expense/start/',
      'getBpmSampleExpenseDetail',
      '/bpm/sample/expense/detail/',
      'markNextBpmSampleExpenseCallbackFailed',
      '/bpm/sample/expense/markNextCallbackFailed/',
      'prepareBpmSampleExpenseDefinition',
      '/bpm/sample/expense/prepareDefinition',
      'BpmSampleExpenseVO',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/sample-expense.ts',
  },
  {
    label: 'integration',
    needles: [
      'queryBpmCallbackRecordPage',
      '/bpm/integration/callback/query',
      'instanceId: data.instanceId',
      'retryBpmCallbackRecord',
      '/bpm/integration/callback/retry/',
      'compensateBpmCallbackRecord',
      '/bpm/integration/callback/compensate/',
      'compensationReason',
      'compensatedAt',
      'compensatedBy',
      'queryBpmCommandRecordPage',
      '/bpm/integration/command/query',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/integration.ts',
  },
] as const;

describe('bpm api 模块', () => {
  it.each(apiFiles)(
    '让 $label API 模块绑定到后端真实 contract',
    ({ needles, path }) => {
      const filePath = resolve(process.cwd(), path);

      expect(existsSync(filePath)).toBe(true);

      const source = readFileSync(filePath, 'utf8');
      needles.forEach((needle) => expect(source).toContain(needle));
    },
  );
});
