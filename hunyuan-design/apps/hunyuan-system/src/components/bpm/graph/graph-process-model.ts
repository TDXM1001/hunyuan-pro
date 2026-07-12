export type GraphNodeType =
  | 'APPROVAL'
  | 'CONDITION'
  | 'COPY'
  | 'END'
  | 'HANDLE'
  | 'INCLUSIVE_GATEWAY'
  | 'PARALLEL_GATEWAY'
  | 'START';

export interface GraphScope {
  name?: string;
  parentScopeId?: string;
  scopeId: string;
}

export interface GraphNode {
  layout?: Record<string, number>;
  name: string;
  nodeId: string;
  properties?: Record<string, unknown>;
  scopeId: string;
  type: GraphNodeType;
}

export interface GraphEdge {
  edgeId: string;
  properties?: Record<string, unknown>;
  scopeId: string;
  sourceNodeId: string;
  sourcePort?: string;
  targetNodeId: string;
}

export interface ProcessDefinitionGraph {
  edges: GraphEdge[];
  nodes: GraphNode[];
  policies: Record<string, unknown>;
  rootScopeId: string;
  schemaVersion: number;
  scopes: GraphScope[];
}

export type GraphDiagnosticSeverity = 'BLOCKING' | 'WARNING';

export interface GraphDiagnostic {
  elementId: string;
  message: string;
  path: string;
  severity: GraphDiagnosticSeverity;
}

export interface GraphSimulationResult {
  findings: GraphDiagnostic[];
  pass: boolean;
}

export interface GraphSemanticChange {
  elementId: string;
  kind: 'EDGE' | 'NODE' | 'POLICY' | 'SCOPE';
  summary: string;
}

export interface GraphSemanticDiff {
  changedElements: GraphSemanticChange[];
  hasSemanticChanges: boolean;
}

/**
 * Graph is a JSON document. JSON round-tripping deliberately removes Vue
 * reactivity before the editor stores an immutable comparison baseline.
 */
export function cloneGraphDocument(graph: ProcessDefinitionGraph): ProcessDefinitionGraph {
  return JSON.parse(JSON.stringify(graph)) as ProcessDefinitionGraph;
}

export function updateGraphBusinessContract(
  graph: ProcessDefinitionGraph,
  patch: Record<string, unknown>,
): ProcessDefinitionGraph {
  const current = asRecord(graph.policies.businessContract) || {};
  const businessContract = {
    contractVersion: positiveInteger(current.contractVersion) ? current.contractVersion : 1,
    ...current,
    ...patch,
  };
  return {
    ...graph,
    policies: { ...graph.policies, businessContract },
  };
}

export function updateGraphCandidatePolicy(
  graph: ProcessDefinitionGraph,
  nodeId: string,
  patch: Record<string, unknown>,
): ProcessDefinitionGraph {
  return {
    ...graph,
    nodes: graph.nodes.map((node) => {
      if (node.nodeId !== nodeId) {
        return node;
      }
      const current = asRecord(node.properties?.candidatePolicy) || {};
      const candidatePolicy = {
        policyVersion: positiveInteger(current.policyVersion) ? current.policyVersion : 1,
        ...current,
        ...patch,
      };
      return {
        ...node,
        properties: { ...node.properties, candidatePolicy },
      };
    }),
  };
}

export function updateGraphApprovalPolicy(
  graph: ProcessDefinitionGraph,
  nodeId: string,
  patch: Record<string, unknown>,
): ProcessDefinitionGraph {
  return {
    ...graph,
    nodes: graph.nodes.map((node) => {
      if (node.nodeId !== nodeId) {
        return node;
      }
      const current = asRecord(node.properties?.approvalPolicy) || {};
      const approvalPolicy = {
        policyVersion: positiveInteger(current.policyVersion) ? current.policyVersion : 1,
        ...current,
        ...patch,
      };
      return {
        ...node,
        properties: { ...node.properties, approvalPolicy },
      };
    }),
  };
}

export function updateGraphStartVisibilityPolicy(
  graph: ProcessDefinitionGraph,
  patch: Record<string, unknown>,
): ProcessDefinitionGraph {
  const current = asRecord(graph.policies.startVisibilityPolicy) || {};
  const startVisibilityPolicy = {
    policyVersion: positiveInteger(current.policyVersion) ? current.policyVersion : 1,
    ...current,
    ...patch,
  };
  return {
    ...graph,
    policies: { ...graph.policies, startVisibilityPolicy },
  };
}

export function updateGraphEdgeRouteCondition(
  graph: ProcessDefinitionGraph,
  edgeId: string,
  patch: Record<string, unknown>,
): ProcessDefinitionGraph {
  return {
    ...graph,
    edges: graph.edges.map((edge) => {
      if (edge.edgeId !== edgeId) {
        return edge;
      }
      const current = asRecord(edge.properties?.routeCondition) || {};
      const routeCondition = {
        sourceType: asText(current.sourceType) || 'FORM_FIELD',
        valueType: asText(current.valueType) || 'NUMBER',
        operator: asText(current.operator) || 'EQ',
        ...current,
        ...patch,
      };
      return {
        ...edge,
        properties: { ...edge.properties, routeCondition },
      };
    }),
  };
}

export function autoLayoutGraph(graph: ProcessDefinitionGraph): ProcessDefinitionGraph {
  const nodesById = new Map(graph.nodes.map((node) => [node.nodeId, node]));
  const outgoing = new Map<string, string[]>();
  const incomingCount = new Map<string, number>();
  const rankByNodeId = new Map<string, number>();

  for (const node of graph.nodes) {
    outgoing.set(node.nodeId, []);
    incomingCount.set(node.nodeId, 0);
  }
  for (const edge of graph.edges) {
    if (!nodesById.has(edge.sourceNodeId) || !nodesById.has(edge.targetNodeId)) {
      continue;
    }
    outgoing.get(edge.sourceNodeId)!.push(edge.targetNodeId);
    incomingCount.set(edge.targetNodeId, (incomingCount.get(edge.targetNodeId) || 0) + 1);
  }

  const queue = graph.nodes
    .filter((node) => (incomingCount.get(node.nodeId) || 0) === 0)
    .map((node) => node.nodeId)
    .sort();
  for (const nodeId of queue) {
    rankByNodeId.set(nodeId, 0);
  }

  while (queue.length > 0) {
    const nodeId = queue.shift()!;
    const rank = rankByNodeId.get(nodeId) || 0;
    for (const targetNodeId of (outgoing.get(nodeId) || []).sort()) {
      rankByNodeId.set(targetNodeId, Math.max(rankByNodeId.get(targetNodeId) || 0, rank + 1));
      const nextCount = (incomingCount.get(targetNodeId) || 1) - 1;
      incomingCount.set(targetNodeId, nextCount);
      if (nextCount === 0) {
        queue.push(targetNodeId);
      }
    }
  }

  const rowsByRank = new Map<number, number>();
  const laidOutNodes = graph.nodes.map((node) => {
    const rank = rankByNodeId.get(node.nodeId) || 0;
    const row = rowsByRank.get(rank) || 0;
    rowsByRank.set(rank, row + 1);
    return {
      ...node,
      layout: { x: 64 + rank * 240, y: 80 + row * 160 },
    };
  });

  return { ...graph, nodes: laidOutNodes };
}

export function semanticFingerprint(graph: ProcessDefinitionGraph): string {
  return stableStringify({
    ...graph,
    edges: [...graph.edges]
      .map(({ properties = {}, ...edge }) => ({ ...edge, properties }))
      .sort((left, right) => left.edgeId.localeCompare(right.edgeId)),
    nodes: [...graph.nodes]
      .map(({ layout: _layout, properties = {}, ...node }) => ({ ...node, properties }))
      .sort((left, right) => left.nodeId.localeCompare(right.nodeId)),
    scopes: [...graph.scopes].sort((left, right) => left.scopeId.localeCompare(right.scopeId)),
  });
}

export function simulateGraph(graph: ProcessDefinitionGraph): GraphSimulationResult {
  const findings: GraphDiagnostic[] = [];
  const outgoing = new Map<string, GraphEdge[]>();
  const nodesById = new Map(graph.nodes.map((node) => [node.nodeId, node]));
  for (const node of graph.nodes) {
    outgoing.set(node.nodeId, []);
  }
  for (const edge of graph.edges) {
    if (nodesById.has(edge.sourceNodeId) && nodesById.has(edge.targetNodeId)) {
      outgoing.get(edge.sourceNodeId)!.push(edge);
    }
  }

  for (const node of graph.nodes) {
    if (!isGateway(node.type)) {
      continue;
    }
    const gatewayMode = asText(node.properties?.gatewayMode);
    const pairedGatewayId = asText(node.properties?.pairedGatewayId);
    if (gatewayMode !== 'SPLIT' && gatewayMode !== 'JOIN') {
      findings.push(graphFinding(node.nodeId, 'gatewayMode', '网关必须设置为分叉或汇合模式'));
      continue;
    }
    const pairedGateway = pairedGatewayId ? nodesById.get(pairedGatewayId) : undefined;
    if (!pairedGateway || pairedGateway.type !== node.type || pairedGateway.scopeId !== node.scopeId) {
      findings.push(graphFinding(node.nodeId, 'pairedGatewayId', '网关必须选择同作用域、同类型的配对网关'));
    } else if (
      asText(pairedGateway.properties?.gatewayMode) === gatewayMode
      || asText(pairedGateway.properties?.pairedGatewayId) !== node.nodeId
    ) {
      findings.push(graphFinding(node.nodeId, 'pairedGatewayId', '配对网关必须双向引用且分别作为分叉和汇合'));
    }

    const nodeOutgoing = outgoing.get(node.nodeId) || [];
    if (gatewayMode === 'SPLIT') {
      if (nodeOutgoing.length < 2) {
        findings.push(graphFinding(node.nodeId, 'outgoing', '分叉网关至少需要两条连线'));
      }
      const ports = new Set<string>();
      const defaultEdges = nodeOutgoing.filter((edge) => edge.sourcePort === 'default');
      for (const edge of nodeOutgoing) {
        const port = edge.sourcePort?.trim();
        if (!port) {
          findings.push(graphFinding(edge.edgeId, 'sourcePort', '分支连线必须配置端口'));
        } else if (ports.has(port)) {
          findings.push(graphFinding(edge.edgeId, 'sourcePort', '同一分叉网关的分支端口不能重复'));
        } else {
          ports.add(port);
        }
        if (isConditionalGateway(node.type) && port !== 'default' && !validRouteCondition(edge.properties?.routeCondition)) {
          findings.push(graphFinding(edge.edgeId, 'routeCondition', '非默认条件分支必须配置类型化路由条件'));
        }
      }
      if (isConditionalGateway(node.type) && defaultEdges.length !== 1) {
        findings.push(graphFinding(node.nodeId, 'defaultRoute', '条件或包容分叉必须且只能有一条默认分支'));
      }
      if (node.type === 'PARALLEL_GATEWAY' && defaultEdges.length > 0) {
        findings.push(graphFinding(node.nodeId, 'defaultRoute', '并行分叉不允许默认分支'));
      }
    }
  }

  for (const node of graph.nodes) {
    if (node.type !== 'APPROVAL' && node.type !== 'HANDLE') {
      continue;
    }
    if (node.type === 'APPROVAL') {
      const approvalPolicy = asRecord(node.properties?.approvalPolicy);
      if (!approvalPolicy || !nonEmptyText(approvalPolicy.policyKey) || !positiveInteger(approvalPolicy.policyVersion)) {
        findings.push({
          elementId: node.nodeId,
          message: '审批节点必须配置已发布的审批策略版本。',
          path: 'approvalPolicy',
          severity: 'BLOCKING',
        });
      }
    }
    const policy = asRecord(node.properties?.candidatePolicy);
    if (!policy || !nonEmptyText(policy.policyKey) || !positiveInteger(policy.policyVersion)) {
      findings.push({
        elementId: node.nodeId,
        message: '审批或办理节点必须配置已发布的候选策略版本。',
        path: 'candidatePolicy',
        severity: 'BLOCKING',
      });
    }
  }

  const businessContract = asRecord(graph.policies.businessContract);
  if (!businessContract || !nonEmptyText(businessContract.contractKey) || !positiveInteger(businessContract.contractVersion)) {
    findings.push({
      elementId: graph.rootScopeId,
      message: '流程必须配置已发布的业务契约版本。',
      path: 'businessContract',
      severity: 'BLOCKING',
    });
  }
  const startVisibilityPolicy = asRecord(graph.policies.startVisibilityPolicy);
  if (!startVisibilityPolicy
    || !nonEmptyText(startVisibilityPolicy.policyKey)
    || !positiveInteger(startVisibilityPolicy.policyVersion)) {
    findings.push({
      elementId: graph.rootScopeId,
      message: '流程必须配置已发布的发起可见范围策略版本。',
      path: 'startVisibilityPolicy',
      severity: 'BLOCKING',
    });
  }
  return { findings, pass: findings.every((finding) => finding.severity !== 'BLOCKING') };
}

export function diffGraphSemantics(
  baseline: ProcessDefinitionGraph,
  current: ProcessDefinitionGraph,
): GraphSemanticDiff {
  const changes: GraphSemanticChange[] = [];
  collectElementChanges(
    baseline.nodes,
    current.nodes,
    'NODE',
    (node) => node.nodeId,
    (node) => semanticNode(node),
    changes,
  );
  collectElementChanges(
    baseline.edges,
    current.edges,
    'EDGE',
    (edge) => edge.edgeId,
    (edge) => edge,
    changes,
  );
  if (stableStringify(baseline.policies) !== stableStringify(current.policies)) {
    changes.push({ elementId: current.rootScopeId, kind: 'POLICY', summary: '流程级策略已变更' });
  }
  return { changedElements: changes, hasSemanticChanges: changes.length > 0 };
}

function collectElementChanges<T>(
  baseline: T[],
  current: T[],
  kind: 'EDGE' | 'NODE',
  id: (value: T) => string,
  semanticValue: (value: T) => unknown,
  changes: GraphSemanticChange[],
) {
  const baselineById = new Map(baseline.map((value) => [id(value), semanticValue(value)]));
  const currentById = new Map(current.map((value) => [id(value), semanticValue(value)]));
  for (const elementId of new Set([...baselineById.keys(), ...currentById.keys()])) {
    if (stableStringify(baselineById.get(elementId)) !== stableStringify(currentById.get(elementId))) {
      changes.push({ elementId, kind, summary: `${kind === 'NODE' ? '节点' : '连线'}语义已变更` });
    }
  }
}

function semanticNode(node: GraphNode) {
  const { layout: _layout, ...semantic } = node;
  return semantic;
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : undefined;
}

function nonEmptyText(value: unknown) {
  return typeof value === 'string' && value.trim().length > 0;
}

function positiveInteger(value: unknown) {
  return typeof value === 'number' && Number.isInteger(value) && value > 0;
}

function graphFinding(elementId: string, path: string, message: string): GraphDiagnostic {
  return { elementId, message, path, severity: 'BLOCKING' };
}

function asText(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim().length > 0 ? value : undefined;
}

function isGateway(type: GraphNodeType): boolean {
  return type === 'CONDITION' || type === 'PARALLEL_GATEWAY' || type === 'INCLUSIVE_GATEWAY';
}

function isConditionalGateway(type: GraphNodeType): boolean {
  return type === 'CONDITION' || type === 'INCLUSIVE_GATEWAY';
}

function validRouteCondition(value: unknown): boolean {
  const condition = asRecord(value);
  if (!condition) {
    return false;
  }
  const sourceType = asText(condition.sourceType);
  if (sourceType === 'REGISTERED_EXPRESSION') {
    return Boolean(asText(condition.expressionKey) && positiveInteger(condition.expressionVersion));
  }
  return (sourceType === 'FORM_FIELD' || sourceType === 'INSTANCE_CONTEXT')
    && Boolean(asText(condition.fieldKey) && asText(condition.valueType) && asText(condition.operator));
}

function stableStringify(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(',')}]`;
  }
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    return `{${Object.keys(record)
      .sort()
      .map((key) => `${JSON.stringify(key)}:${stableStringify(record[key])}`)
      .join(',')}}`;
  }
  return JSON.stringify(value);
}
