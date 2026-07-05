import { describe, expect, it } from 'vitest';

import {
  buildReadonlyBpmnXml,
  parseSimpleModelDraft,
  stringifySimpleModelDraft,
} from './simple-model-bridge';

describe('bpm designer adapters', () => {
  it('把后端 simpleModelJson 解析为受约束的节点草稿数组', () => {
    expect(
      parseSimpleModelDraft(
        JSON.stringify({
          nodes: [
            {
              approvalMode: 'single',
              candidateResolverType: 'EMPLOYEE',
              id: 'task_apply',
              listeners: [
                {
                  channels: ['MESSAGE'],
                  listenerCode: 'notify_message',
                },
              ],
              name: '部门负责人审批',
              type: 'userTask',
            },
          ],
        }),
      ),
    ).toEqual([
      {
        approvalMode: 'single',
        candidateResolverType: 'EMPLOYEE',
        id: 'task_apply',
        listeners: [
          {
            channels: ['MESSAGE'],
            listenerCode: 'notify_message',
          },
        ],
        name: '部门负责人审批',
        nodeKey: 'task_apply',
        type: 'userTask',
      },
    ]);
  });

  it('把节点草稿重新序列化为当前后端可接受的 simpleModelJson', () => {
    expect(
      stringifySimpleModelDraft([
        {
          approvalMode: 'single',
          candidateResolverType: 'ROLE',
          id: 'task_finance',
          listeners: [],
          name: '财务审批',
          nodeKey: 'task_finance',
          type: 'userTask',
        },
      ]),
    ).toBe(
      '{"nodes":[{"id":"task_finance","nodeKey":"task_finance","name":"财务审批","type":"userTask","approvalMode":"single","candidateResolverType":"ROLE","listeners":[]}]}',
    );
  });

  it('根据顺序审批节点生成只读 BPMN XML 预览', () => {
    const xml = buildReadonlyBpmnXml('leave_apply', '请假流程', [
      {
        approvalMode: 'single',
        candidateResolverType: 'DEPARTMENT_MANAGER',
        id: 'task_manager',
        listeners: [],
        name: '主管审批',
        nodeKey: 'task_manager',
        type: 'userTask',
      },
    ]);

    expect(xml).toContain('<process id="leave_apply"');
    expect(xml).toContain('<userTask id="task_manager"');
    expect(xml).toContain('flowable:assignee="${assignee_task_manager}"');
  });
});
