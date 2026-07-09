import type { BpmProcessNodeDraft } from './types';

import { describe, expect, it } from 'vitest';

import { extractEmployeeSelectFieldOptions } from './employee-select-field-options';
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

  it('保留发起人相关候选策略的 simpleModelJson 合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'single',
            candidateResolverType: 'START_EMPLOYEE',
            id: 'task_self',
            listeners: [],
            name: '发起人自审',
            type: 'userTask',
          },
          {
            approvalMode: 'single',
            candidateResolverType: 'START_DEPARTMENT_MANAGER',
            id: 'task_start_manager',
            listeners: [],
            name: '发起人部门主管审批',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes.map((item) => item.candidateResolverType)).toEqual([
      'START_EMPLOYEE',
      'START_DEPARTMENT_MANAGER',
    ]);

    const startDepartmentManagerNode: BpmProcessNodeDraft = {
      approvalMode: 'single',
      candidateResolverType: 'START_DEPARTMENT_MANAGER',
      id: 'task_start_manager',
      listeners: [],
      name: '发起人部门主管审批',
      nodeKey: 'task_start_manager',
      type: 'userTask',
    };

    expect(stringifySimpleModelDraft([startDepartmentManagerNode])).toContain(
      '"candidateResolverType":"START_DEPARTMENT_MANAGER"',
    );
  });

  it('保留发起时自选审批人的字段 key 合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'single',
            candidateResolverType: 'EMPLOYEE_SELECT_AT_START',
            employeeSelectFieldKey: 'approverEmployeeId',
            id: 'task_selected',
            listeners: [],
            name: '发起时选择审批',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes).toEqual([
      {
        approvalMode: 'single',
        candidateResolverType: 'EMPLOYEE_SELECT_AT_START',
        employeeSelectFieldKey: 'approverEmployeeId',
        id: 'task_selected',
        listeners: [],
        name: '发起时选择审批',
        nodeKey: 'task_selected',
        type: 'userTask',
      },
    ]);

    expect(stringifySimpleModelDraft(parsedNodes)).toContain(
      '"employeeSelectFieldKey":"approverEmployeeId"',
    );
  });

  it('从表单 schema 提取员工单选字段候选项', () => {
    const options = extractEmployeeSelectFieldOptions(
      JSON.stringify({
        fields: [
          {
            field: 'approverEmployeeId',
            title: '审批人',
            type: 'employeeSelect',
          },
          {
            field: 'amount',
            title: '金额',
            type: 'input',
          },
        ],
      }),
    );

    expect(options).toEqual([
      {
        field: 'approverEmployeeId',
        label: '审批人',
      },
    ]);
  });

  it('兼容字段名带员工语义但组件类型尚未标准化的表单字段', () => {
    const options = extractEmployeeSelectFieldOptions(
      JSON.stringify([
        {
          field: 'backupApproverEmployeeId',
          title: '备选审批人',
          type: 'input',
        },
      ]),
    );

    expect(options).toEqual([
      {
        field: 'backupApproverEmployeeId',
        label: '备选审批人',
      },
    ]);
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
