import type { BpmProcessNodeDraft } from './types';

interface PreviewElement {
  height: number;
  id: string;
  name: string;
  type: 'endEvent' | 'parallelGateway' | 'startEvent' | 'userTask';
  width: number;
  x: number;
  y: number;
}

interface PreviewFlow {
  id: string;
  sourceRef: string;
  targetRef: string;
}

function normalizeEmployeeIds(rawValue: unknown): number[] {
  if (!Array.isArray(rawValue)) {
    return [];
  }

  return rawValue
    .map((item) => Number(item))
    .filter((item) => Number.isSafeInteger(item) && item > 0);
}

function normalizePositiveId(rawValue: unknown): number | undefined {
  const normalizedValue = Number(rawValue);
  return Number.isSafeInteger(normalizedValue) && normalizedValue > 0
    ? normalizedValue
    : undefined;
}

function normalizeFieldPermissions(rawValue: unknown) {
  if (!Array.isArray(rawValue)) {
    return [];
  }
  return rawValue
    .filter(
      (item) =>
        item &&
        typeof item.fieldKey === 'string' &&
        ['EDITABLE', 'HIDDEN', 'READONLY'].includes(item.permission),
    )
    .map((item) => ({
      fieldKey: String(item.fieldKey).trim(),
      permission: item.permission as 'EDITABLE' | 'HIDDEN' | 'READONLY',
      required: item.permission === 'EDITABLE' && item.required === true,
    }))
    .filter((item) => item.fieldKey);
}

function normalizeNode(rawNode: Record<string, any>): BpmProcessNodeDraft {
  const nodeKey = String(rawNode.nodeKey || rawNode.id || '').trim();
  const nodeId = String(rawNode.id || rawNode.nodeKey || '').trim() || nodeKey;
  const employeeIds = normalizeEmployeeIds(rawNode.employeeIds);
  const employeeSelectFieldKey =
    typeof rawNode.employeeSelectFieldKey === 'string'
      ? rawNode.employeeSelectFieldKey.trim()
      : '';
  const fieldPermissions = normalizeFieldPermissions(rawNode.fieldPermissions);

  return {
    approvalMode: rawNode.approvalMode || 'single',
    candidateResolverType:
      rawNode.candidateResolverType || rawNode.resolverType || 'EMPLOYEE',
    ...(normalizePositiveId(rawNode.departmentId)
      ? { departmentId: normalizePositiveId(rawNode.departmentId) }
      : {}),
    ...(normalizePositiveId(rawNode.employeeId)
      ? { employeeId: normalizePositiveId(rawNode.employeeId) }
      : {}),
    ...(employeeIds.length ? { employeeIds } : {}),
    ...(employeeSelectFieldKey ? { employeeSelectFieldKey } : {}),
    ...(fieldPermissions.length ? { fieldPermissions } : {}),
    id: nodeId,
    listeners: Array.isArray(rawNode.listeners) ? rawNode.listeners : [],
    name: String(rawNode.name || '审批节点').trim(),
    nodeKey: nodeKey || nodeId,
    ...(normalizePositiveId(rawNode.roleId)
      ? { roleId: normalizePositiveId(rawNode.roleId) }
      : {}),
    type: 'userTask',
  };
}

export function parseSimpleModelDraft(jsonText: string): BpmProcessNodeDraft[] {
  if (!jsonText.trim()) {
    return [];
  }

  const parsed = JSON.parse(jsonText);
  const nodes = Array.isArray(parsed?.nodes)
    ? (parsed.nodes as Record<string, any>[])
    : [];

  return nodes
    .filter(
      (item: Record<string, any>) => item && (item.type || 'userTask') === 'userTask',
    )
    .map((item: Record<string, any>) => normalizeNode(item));
}

export function stringifySimpleModelDraft(nodes: BpmProcessNodeDraft[]): string {
  return JSON.stringify({
    nodes: nodes.map((node) => ({
      id: node.id,
      nodeKey: node.nodeKey,
      name: node.name,
      type: 'userTask',
      approvalMode: node.approvalMode || 'single',
      candidateResolverType: node.candidateResolverType || 'EMPLOYEE',
      ...(node.departmentId ? { departmentId: node.departmentId } : {}),
      ...(node.employeeId ? { employeeId: node.employeeId } : {}),
      ...((node.approvalMode === 'sequential' ||
        node.approvalMode === 'parallelAll') &&
      node.employeeIds?.length
        ? { employeeIds: node.employeeIds }
        : {}),
      ...(node.employeeSelectFieldKey
        ? { employeeSelectFieldKey: node.employeeSelectFieldKey }
        : {}),
      ...(node.fieldPermissions?.length
        ? { fieldPermissions: node.fieldPermissions }
        : {}),
      ...(node.roleId ? { roleId: node.roleId } : {}),
      listeners: node.listeners || [],
    })),
  });
}

function escapeXmlAttribute(value: unknown): string {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

export function buildReadonlyBpmnXml(
  modelKey: string,
  modelName: string,
  nodes: BpmProcessNodeDraft[],
) {
  const normalizedKey = modelKey || 'process_model';
  const normalizedName = modelName || '流程模型';
  let previousRef = 'startEvent';
  let flowIndex = 0;
  let cursorX = 190;
  const fragmentXml: string[] = [];
  const previewElements: PreviewElement[] = [
    {
      height: 36,
      id: 'startEvent',
      name: '开始',
      type: 'startEvent',
      width: 36,
      x: 80,
      y: 222,
    },
  ];
  const previewFlows: PreviewFlow[] = [];
  const centerY = 240;

  const appendFlow = (
    id: string,
    sourceRef: string,
    targetRef: string,
  ) => {
    fragmentXml.push(
      `<sequenceFlow id="${escapeXmlAttribute(id)}" sourceRef="${escapeXmlAttribute(sourceRef)}" targetRef="${escapeXmlAttribute(targetRef)}"/>`,
    );
    previewFlows.push({ id, sourceRef, targetRef });
  };

  nodes.forEach((node) => {
    const employeeIds = node.employeeIds ?? [];
    if (node.approvalMode === 'parallelAll' && employeeIds.length) {
      const splitGatewayKey = `gateway_${node.nodeKey}_split`;
      const joinGatewayKey = `gateway_${node.nodeKey}_join`;
      const memberX = cursorX + 140;
      const joinX = cursorX + 340;
      fragmentXml.push(
        `<parallelGateway id="${escapeXmlAttribute(splitGatewayKey)}" name="${escapeXmlAttribute(`${node.name}分叉`)}"/>`,
        `<parallelGateway id="${escapeXmlAttribute(joinGatewayKey)}" name="${escapeXmlAttribute(`${node.name}汇聚`)}"/>`,
      );
      previewElements.push(
        {
          height: 50,
          id: splitGatewayKey,
          name: `${node.name}分叉`,
          type: 'parallelGateway',
          width: 50,
          x: cursorX,
          y: centerY - 25,
        },
        {
          height: 50,
          id: joinGatewayKey,
          name: `${node.name}汇聚`,
          type: 'parallelGateway',
          width: 50,
          x: joinX,
          y: centerY - 25,
        },
      );
      appendFlow(`flow_${flowIndex++}`, previousRef, splitGatewayKey);
      employeeIds.forEach((_, index) => {
        const memberKey = `${node.nodeKey}_${index + 1}`;
        const memberName = `${node.name}（${index + 1}/${employeeIds.length}）`;
        const memberCenterY =
          centerY + (index - (employeeIds.length - 1) / 2) * 120;
        fragmentXml.push(
          `<userTask id="${escapeXmlAttribute(memberKey)}" name="${escapeXmlAttribute(memberName)}" flowable:assignee="${escapeXmlAttribute(`\${assignee_${memberKey}}`)}"/>`,
        );
        previewElements.push({
          height: 80,
          id: memberKey,
          name: memberName,
          type: 'userTask',
          width: 120,
          x: memberX,
          y: memberCenterY - 40,
        });
        appendFlow(`flow_${flowIndex++}`, splitGatewayKey, memberKey);
        appendFlow(`flow_${flowIndex++}`, memberKey, joinGatewayKey);
      });
      previousRef = joinGatewayKey;
      cursorX = joinX + 190;
      return;
    }

    const taskNodes =
      node.approvalMode === 'sequential' && employeeIds.length
        ? employeeIds.map((_, index) => ({
            nodeKey: `${node.nodeKey}_${index + 1}`,
            nodeName: `${node.name}（${index + 1}/${employeeIds.length}）`,
          }))
        : [{ nodeKey: node.nodeKey, nodeName: node.name }];
    taskNodes.forEach((taskNode) => {
      fragmentXml.push(
        `<userTask id="${escapeXmlAttribute(taskNode.nodeKey)}" name="${escapeXmlAttribute(taskNode.nodeName)}" flowable:assignee="${escapeXmlAttribute(`\${assignee_${taskNode.nodeKey}}`)}"/>`,
      );
      previewElements.push({
        height: 80,
        id: taskNode.nodeKey,
        name: taskNode.nodeName,
        type: 'userTask',
        width: 120,
        x: cursorX,
        y: centerY - 40,
      });
      appendFlow(`flow_${flowIndex++}`, previousRef, taskNode.nodeKey);
      previousRef = taskNode.nodeKey;
      cursorX += 190;
    });
  });

  previewElements.push({
    height: 36,
    id: 'endEvent',
    name: '结束',
    type: 'endEvent',
    width: 36,
    x: cursorX,
    y: centerY - 18,
  });
  appendFlow('flow_end', previousRef, 'endEvent');

  const elementById = new Map(
    previewElements.map((element) => [element.id, element]),
  );
  const shapeXml = previewElements
    .map(
      (element) => `<bpmndi:BPMNShape id="${escapeXmlAttribute(`${element.id}_di`)}" bpmnElement="${escapeXmlAttribute(element.id)}">
        <dc:Bounds x="${element.x}" y="${element.y}" width="${element.width}" height="${element.height}"/>
      </bpmndi:BPMNShape>`,
    )
    .join('');
  const edgeXml = previewFlows
    .map((flow) => {
      const source = elementById.get(flow.sourceRef);
      const target = elementById.get(flow.targetRef);
      if (!source || !target) {
        return '';
      }

      // 只读预览使用元素左右中心点连线，保证固定拓扑可被 bpmn-js 稳定导入和缩放。
      const sourceX = source.x + source.width;
      const sourceY = source.y + source.height / 2;
      const targetX = target.x;
      const targetY = target.y + target.height / 2;
      return `<bpmndi:BPMNEdge id="${escapeXmlAttribute(`${flow.id}_di`)}" bpmnElement="${escapeXmlAttribute(flow.id)}">
        <di:waypoint x="${sourceX}" y="${sourceY}"/>
        <di:waypoint x="${targetX}" y="${targetY}"/>
      </bpmndi:BPMNEdge>`;
    })
    .join('');

  return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://hunyuan.sa/bpm">
  <process id="${escapeXmlAttribute(normalizedKey)}" name="${escapeXmlAttribute(normalizedName)}" isExecutable="true">
    <startEvent id="startEvent" name="开始"/>
    ${fragmentXml.join('')}
    <endEvent id="endEvent" name="结束"/>
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_${escapeXmlAttribute(normalizedKey)}">
    <bpmndi:BPMNPlane id="BPMNPlane_${escapeXmlAttribute(normalizedKey)}" bpmnElement="${escapeXmlAttribute(normalizedKey)}">
      ${shapeXml}
      ${edgeXml}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;
}
