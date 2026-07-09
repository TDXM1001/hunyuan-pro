import type { BpmProcessNodeDraft } from './types';

function normalizeNode(rawNode: Record<string, any>): BpmProcessNodeDraft {
  const nodeKey = String(rawNode.nodeKey || rawNode.id || '').trim();
  const nodeId = String(rawNode.id || rawNode.nodeKey || '').trim() || nodeKey;

  return {
    approvalMode: rawNode.approvalMode || 'single',
    candidateResolverType:
      rawNode.candidateResolverType || rawNode.resolverType || 'EMPLOYEE',
    employeeSelectFieldKey:
      typeof rawNode.employeeSelectFieldKey === 'string'
        ? rawNode.employeeSelectFieldKey.trim()
        : undefined,
    id: nodeId,
    listeners: Array.isArray(rawNode.listeners) ? rawNode.listeners : [],
    name: String(rawNode.name || '审批节点').trim(),
    nodeKey: nodeKey || nodeId,
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
      ...(node.employeeSelectFieldKey
        ? { employeeSelectFieldKey: node.employeeSelectFieldKey }
        : {}),
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
  const taskXml = nodes
    .map((node, index) => {
      const sourceRef = index === 0 ? 'startEvent' : nodes[index - 1]!.nodeKey;

      return [
        `<userTask id="${node.nodeKey}" name="${node.name}" flowable:assignee="\${assignee_${node.nodeKey}}"/>`,
        `<sequenceFlow id="flow_${index}" sourceRef="${sourceRef}" targetRef="${node.nodeKey}"/>`,
      ].join('');
    })
    .join('');
  const endSource = nodes.length ? nodes[nodes.length - 1]!.nodeKey : 'startEvent';

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
