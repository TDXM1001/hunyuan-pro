import type { BpmProcessNodeDraft } from './types';

import { describe, expect, it } from 'vitest';

import { extractEmployeeSelectFieldOptions } from './employee-select-field-options';
import {
  parseProcessModelAsset,
  stringifyProcessModelAsset,
} from './process-model-asset';
import {
  buildReadonlyBpmnXml,
  parseSimpleModelDraft,
  stringifySimpleModelDraft,
} from './simple-model-bridge';

describe('bpm designer adapters', () => {
  it('完整往返嵌套 v2 分支并保留创作身份', () => {
    const source = JSON.stringify({
      schemaVersion: 2,
      nodes: [
        {
          nodeKey: 'amount_route',
          name: '金额路由',
          type: 'EXCLUSIVE_BRANCH',
          branches: [
            {
              branchKey: 'large',
              name: '大额',
              condition: { fieldKey: 'amount', operator: 'GT', value: 5000 },
              nodes: [
                {
                  nodeKey: 'risk_handle',
                  name: '风险办理',
                  type: 'HANDLE_TASK',
                  candidateResolverType: 'ROLE',
                  roleId: 7,
                },
              ],
            },
            { branchKey: 'default', name: '默认', isDefault: true, nodes: [] },
          ],
        },
      ],
    });

    const asset = parseProcessModelAsset(source);

    expect(parseProcessModelAsset(stringifyProcessModelAsset(asset))).toEqual(asset);
  });

  it.each([3, 99])('覆盖草稿前拒绝不支持的 schemaVersion %s', (version) => {
    expect(() =>
      parseProcessModelAsset(JSON.stringify({ schemaVersion: version, nodes: [] })),
    ).toThrow('不支持的流程模型版本');
  });

  it('拒绝递归结构中的重复节点 key', () => {
    expect(() =>
      parseProcessModelAsset(
        JSON.stringify({
          schemaVersion: 2,
          nodes: [
            { nodeKey: 'same', name: '节点一', type: 'USER_TASK' },
            { nodeKey: 'same', name: '节点二', type: 'COPY_TASK' },
          ],
        }),
      ),
    ).toThrow('节点 key 重复');
  });
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
    const serialized = stringifySimpleModelDraft([
        {
          approvalMode: 'single',
          candidateResolverType: 'ROLE',
          id: 'task_finance',
          listeners: [],
          name: '财务审批',
          nodeKey: 'task_finance',
          type: 'userTask',
        },
      ]);
    expect(JSON.parse(serialized)).toMatchObject({
      schemaVersion: 2,
      nodes: [
        {
          candidateResolverType: 'ROLE',
          nodeKey: 'task_finance',
          type: 'USER_TASK',
        },
      ],
    });
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

  it('保留顺序多人审批的员工列表合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'sequential',
            candidateResolverType: 'EMPLOYEE',
            employeeIds: [301, 302],
            id: 'task_finance',
            listeners: [],
            name: '财务复核',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes).toEqual([
      {
        approvalMode: 'sequential',
        candidateResolverType: 'EMPLOYEE',
        employeeIds: [301, 302],
        id: 'task_finance',
        listeners: [],
        name: '财务复核',
        nodeKey: 'task_finance',
        type: 'userTask',
      },
    ]);

    expect(stringifySimpleModelDraft(parsedNodes)).toContain(
      '"employeeIds":[301,302]',
    );
  });

  it('保留并行全员会签的员工列表合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'parallelAll',
            candidateResolverType: 'EMPLOYEE',
            employeeIds: [101, 102],
            id: 'finance_review',
            listeners: [],
            name: '财务会签',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes).toEqual([
      {
        approvalMode: 'parallelAll',
        candidateResolverType: 'EMPLOYEE',
        employeeIds: [101, 102],
        id: 'finance_review',
        listeners: [],
        name: '财务会签',
        nodeKey: 'finance_review',
        type: 'userTask',
      },
    ]);
    expect(stringifySimpleModelDraft(parsedNodes)).toContain(
      '"approvalMode":"parallelAll"',
    );
    expect(stringifySimpleModelDraft(parsedNodes)).toContain(
      '"employeeIds":[101,102]',
    );
  });

  it('往返保留节点字段权限合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'single',
            candidateResolverType: 'EMPLOYEE',
            fieldPermissions: [
              {
                fieldKey: 'approvedAmount',
                permission: 'EDITABLE',
                required: true,
              },
              {
                fieldKey: 'internalCode',
                permission: 'HIDDEN',
                required: false,
              },
            ],
            id: 'finance_review',
            listeners: [],
            name: '财务复核',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes[0]?.fieldPermissions).toEqual([
      {
        fieldKey: 'approvedAmount',
        permission: 'EDITABLE',
        required: true,
      },
      {
        fieldKey: 'internalCode',
        permission: 'HIDDEN',
        required: false,
      },
    ]);
    expect(stringifySimpleModelDraft(parsedNodes)).toContain(
      '"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"EDITABLE","required":true}',
    );
  });

  it('保留指定员工、角色和指定部门主管的候选参数', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'single',
            candidateResolverType: 'EMPLOYEE',
            employeeId: 301,
            id: 'task_employee',
            listeners: [],
            name: '指定员工审批',
            type: 'userTask',
          },
          {
            approvalMode: 'single',
            candidateResolverType: 'ROLE',
            id: 'task_role',
            listeners: [],
            name: '角色审批',
            roleId: 9,
            type: 'userTask',
          },
          {
            approvalMode: 'single',
            candidateResolverType: 'DEPARTMENT_MANAGER',
            departmentId: 8,
            id: 'task_department',
            listeners: [],
            name: '指定部门主管审批',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes[0]?.employeeId).toBe(301);
    expect(parsedNodes[1]?.roleId).toBe(9);
    expect(parsedNodes[2]?.departmentId).toBe(8);

    const serializedDraft = stringifySimpleModelDraft(parsedNodes);
    expect(serializedDraft).toContain('"employeeId":301');
    expect(serializedDraft).toContain('"roleId":9');
    expect(serializedDraft).toContain('"departmentId":8');
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

  it('不根据字段名猜测普通 input 是员工字段', () => {
    const options = extractEmployeeSelectFieldOptions(
      JSON.stringify([
        {
          field: 'backupApproverEmployeeId',
          title: '备选审批人',
          type: 'input',
        },
      ]),
    );

    expect(options).toEqual([]);
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
    expect(xml).toContain(
      '<bpmndi:BPMNPlane id="BPMNPlane_leave_apply" bpmnElement="leave_apply">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNShape id="task_manager_di" bpmnElement="task_manager">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNEdge id="flow_0_di" bpmnElement="flow_0">',
    );
  });

  it('空草稿仍生成可导入的 BPMN DI 预览', () => {
    const xml = buildReadonlyBpmnXml('empty_process', '空流程', []);

    expect(xml).toContain(
      '<bpmndi:BPMNDiagram id="BPMNDiagram_empty_process">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNEdge id="flow_end_di" bpmnElement="flow_end">',
    );
  });

  it('根据顺序多人审批节点展开只读 BPMN XML 预览', () => {
    const xml = buildReadonlyBpmnXml('expense_apply', '费用流程', [
      {
        approvalMode: 'sequential',
        candidateResolverType: 'EMPLOYEE',
        employeeIds: [301, 302],
        id: 'task_finance',
        listeners: [],
        name: '财务复核',
        nodeKey: 'task_finance',
        type: 'userTask',
      },
    ]);

    expect(xml).toContain('<userTask id="task_finance_1"');
    expect(xml).toContain('<userTask id="task_finance_2"');
    expect(xml).toContain('财务复核（1/2）');
    expect(xml).toContain('flowable:assignee="${assignee_task_finance_2}"');
  });

  it('根据并行全员会签节点生成固定分叉和汇聚预览', () => {
    const xml = buildReadonlyBpmnXml('expense_apply', '费用流程', [
      {
        approvalMode: 'parallelAll',
        candidateResolverType: 'EMPLOYEE',
        employeeIds: [101, 102],
        id: 'finance_review',
        listeners: [],
        name: '财务会签',
        nodeKey: 'finance_review',
        type: 'userTask',
      },
    ]);

    expect(xml).toContain('<parallelGateway id="gateway_finance_review_split"');
    expect(xml).toContain('<userTask id="finance_review_1"');
    expect(xml).toContain('<userTask id="finance_review_2"');
    expect(xml).toContain('<parallelGateway id="gateway_finance_review_join"');
    expect(xml).toContain('sourceRef="gateway_finance_review_split"');
    expect(xml).toContain('targetRef="gateway_finance_review_join"');
    expect(xml).toContain(
      '<bpmndi:BPMNShape id="gateway_finance_review_split_di" bpmnElement="gateway_finance_review_split">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNShape id="finance_review_1_di" bpmnElement="finance_review_1">',
    );
    expect(xml).toContain(
      '<bpmndi:BPMNShape id="gateway_finance_review_join_di" bpmnElement="gateway_finance_review_join">',
    );
  });
});
