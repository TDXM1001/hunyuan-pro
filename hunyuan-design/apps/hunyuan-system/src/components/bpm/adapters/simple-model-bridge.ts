import type { BpmProcessNodeDraft } from './types';

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

function normalizeNode(rawNode: Record<string, any>): BpmProcessNodeDraft {
  const nodeKey = String(rawNode.nodeKey || rawNode.id || '').trim();
  const nodeId = String(rawNode.id || rawNode.nodeKey || '').trim() || nodeKey;
  const employeeIds = normalizeEmployeeIds(rawNode.employeeIds);
  const employeeSelectFieldKey =
    typeof rawNode.employeeSelectFieldKey === 'string'
      ? rawNode.employeeSelectFieldKey.trim()
      : '';

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
      ...(node.approvalMode === 'sequential' && node.employeeIds?.length
        ? { employeeIds: node.employeeIds }
        : {}),
      ...(node.employeeSelectFieldKey
        ? { employeeSelectFieldKey: node.employeeSelectFieldKey }
        : {}),
      ...(node.roleId ? { roleId: node.roleId } : {}),
      listeners: node.listeners || [],
    })),
  });
}

export function buildReadonlyBpmnXml(
  modelKey: string,
  modelName: string,
  nodes: BpmProcessNodeDraft[],
) {
  const normalizedKey = modelKey || 'process_model';
  const normalizedName = modelName || '流程模型';
  const previewNodes = nodes.flatMap((node) => {
    if (node.approvalMode !== 'sequential' || !node.employeeIds?.length) {
      return [{ nodeKey: node.nodeKey, nodeName: node.name }];
    }

    return node.employeeIds.map((_, index) => ({
      nodeKey: `${node.nodeKey}_${index + 1}`,
      nodeName: `${node.name}（${index + 1}/${node.employeeIds!.length}）`,
    }));
  });
  const taskXml = previewNodes
    .map((node, index) => {
      const sourceRef =
        index === 0 ? 'startEvent' : previewNodes[index - 1]!.nodeKey;

      return [
        `<userTask id="${node.nodeKey}" name="${node.nodeName}" flowable:assignee="\${assignee_${node.nodeKey}}"/>`,
        `<sequenceFlow id="flow_${index}" sourceRef="${sourceRef}" targetRef="${node.nodeKey}"/>`,
      ].join('');
    })
    .join('');
  const endSource = previewNodes.length
    ? previewNodes[previewNodes.length - 1]!.nodeKey
    : 'startEvent';

  return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://hunyuan.sa/bpm">
  <process id="${normalizedKey}" name="${normalizedName}" isExecutable="true">
    <startEvent id="startEvent" name="开始"/>
    ${taskXml}
    <endEvent id="endEvent" name="结束"/>
    <sequenceFlow id="flow_end" sourceRef="${endSource}" targetRef="endEvent"/>
  </process>
</definitions>`;
}
