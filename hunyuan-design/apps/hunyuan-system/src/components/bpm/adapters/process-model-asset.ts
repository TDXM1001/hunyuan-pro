import type {
  BpmBranchNodeDraft,
  BpmCandidateResolverType,
  BpmProcessBranchDraft,
  BpmProcessModelAsset,
  BpmProcessNodeDraft,
} from './types';

const CANDIDATE_TYPES = new Set<BpmCandidateResolverType>([
  'DEPARTMENT_MANAGER',
  'EMPLOYEE',
  'EMPLOYEE_SELECT_AT_START',
  'ROLE',
  'START_DEPARTMENT_MANAGER',
  'START_EMPLOYEE',
]);

function requireObject(value: unknown, label: string): Record<string, any> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${label}必须是 JSON object`);
  }
  return value as Record<string, any>;
}

function requireKey(value: unknown, label: string) {
  const result = String(value || '').trim();
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(result)) {
    throw new Error(`${label}不合法`);
  }
  return result;
}

function normalizeCommon(raw: Record<string, any>, nodeKey: string) {
  const resolver = raw.candidateResolverType || raw.resolverType;
  if (resolver && !CANDIDATE_TYPES.has(resolver)) {
    throw new Error(`节点【${nodeKey}】接收人类型不受支持`);
  }
  return {
    ...(raw.approvalMode ? { approvalMode: raw.approvalMode } : {}),
    ...(resolver ? { candidateResolverType: resolver } : {}),
    ...(Number.isSafeInteger(Number(raw.departmentId)) && Number(raw.departmentId) > 0
      ? { departmentId: Number(raw.departmentId) }
      : {}),
    ...(Number.isSafeInteger(Number(raw.employeeId)) && Number(raw.employeeId) > 0
      ? { employeeId: Number(raw.employeeId) }
      : {}),
    ...(Array.isArray(raw.employeeIds)
      ? { employeeIds: raw.employeeIds.map(Number) }
      : {}),
    ...(typeof raw.employeeSelectFieldKey === 'string'
      ? { employeeSelectFieldKey: raw.employeeSelectFieldKey }
      : {}),
    ...(Array.isArray(raw.fieldPermissions)
      ? { fieldPermissions: raw.fieldPermissions }
      : {}),
    id: String(raw.id || nodeKey),
    listeners: Array.isArray(raw.listeners) ? raw.listeners : [],
    name: String(raw.name || nodeKey).trim(),
    nodeKey,
    ...(Number.isSafeInteger(Number(raw.roleId)) && Number(raw.roleId) > 0
      ? { roleId: Number(raw.roleId) }
      : {}),
  };
}

function parseNodes(
  rawNodes: unknown,
  depth: number,
  nodeKeys: Set<string>,
  branchKeys: Set<string>,
): BpmProcessNodeDraft[] {
  if (!Array.isArray(rawNodes)) {
    throw new Error('流程模型 nodes 必须是数组');
  }
  if (depth > 3) {
    throw new Error('流程分支嵌套深度不能超过 3');
  }
  return rawNodes.map((rawValue, index) => {
    const raw = requireObject(rawValue, `第 ${index + 1} 个节点`);
    const nodeKey = requireKey(raw.nodeKey || raw.id, '节点 key');
    if (nodeKeys.has(nodeKey)) {
      throw new Error(`节点 key 重复：${nodeKey}`);
    }
    nodeKeys.add(nodeKey);
    const common = normalizeCommon(raw, nodeKey);
    const rawType = String(raw.type || 'userTask');
    if (rawType === 'userTask' || rawType === 'USER_TASK') {
      return { ...common, type: 'userTask' };
    }
    if (rawType === 'HANDLE_TASK') {
      return { ...common, type: 'handleTask' };
    }
    if (rawType === 'COPY_TASK') {
      return { ...common, type: 'copyTask' };
    }
    const branchTypeByNodeType = {
      EXCLUSIVE_BRANCH: 'EXCLUSIVE',
      INCLUSIVE_BRANCH: 'INCLUSIVE',
      PARALLEL_BRANCH: 'PARALLEL',
    } as const;
    const branchType = branchTypeByNodeType[rawType as keyof typeof branchTypeByNodeType];
    if (!branchType) {
      throw new Error(`不支持的流程节点类型：${rawType}`);
    }
    if (!Array.isArray(raw.branches) || raw.branches.length < 2) {
      throw new Error(`分支节点【${nodeKey}】至少需要两个分支`);
    }
    const branches: BpmProcessBranchDraft[] = raw.branches.map(
      (rawBranch: unknown, branchIndex: number) => {
        const branch = requireObject(rawBranch, `节点【${nodeKey}】第 ${branchIndex + 1} 个分支`);
        const branchKey = requireKey(branch.branchKey, '分支 key');
        if (branchKeys.has(branchKey)) {
          throw new Error(`分支 key 重复：${branchKey}`);
        }
        branchKeys.add(branchKey);
        return {
          branchKey,
          ...(branch.condition ? { condition: branch.condition } : {}),
          ...(branch.isDefault === true ? { isDefault: true } : {}),
          name: String(branch.name || branchKey).trim(),
          nodes: parseNodes(branch.nodes || [], depth + 1, nodeKeys, branchKeys),
        };
      },
    );
    return { ...common, branches, branchType, type: 'branch' } as BpmBranchNodeDraft;
  });
}

export function parseProcessModelAsset(jsonText: string): BpmProcessModelAsset {
  let parsed: unknown;
  try {
    parsed = JSON.parse(jsonText || '{}');
  } catch {
    throw new Error('流程模型不是合法 JSON');
  }
  const root = requireObject(parsed, '流程模型');
  const schemaVersion = root.schemaVersion == null ? 1 : Number(root.schemaVersion);
  if (schemaVersion !== 1 && schemaVersion !== 2) {
    throw new Error(`不支持的流程模型版本：${schemaVersion}`);
  }
  return {
    nodes: parseNodes(root.nodes || [], 0, new Set(), new Set()),
    schemaVersion: 2,
  };
}

function toBackendNode(node: BpmProcessNodeDraft): Record<string, unknown> {
  const common = {
    ...(node.approvalMode ? { approvalMode: node.approvalMode } : {}),
    ...(node.candidateResolverType
      ? { candidateResolverType: node.candidateResolverType }
      : {}),
    ...(node.departmentId ? { departmentId: node.departmentId } : {}),
    ...(node.employeeId ? { employeeId: node.employeeId } : {}),
    ...(node.employeeIds?.length ? { employeeIds: node.employeeIds } : {}),
    ...(node.employeeSelectFieldKey
      ? { employeeSelectFieldKey: node.employeeSelectFieldKey }
      : {}),
    ...(node.fieldPermissions?.length
      ? { fieldPermissions: node.fieldPermissions }
      : {}),
    id: node.id,
    listeners: node.listeners || [],
    name: node.name,
    nodeKey: node.nodeKey,
    ...(node.roleId ? { roleId: node.roleId } : {}),
  };
  if (node.type === 'branch') {
    return {
      ...common,
      branches: node.branches.map((branch) => ({
        branchKey: branch.branchKey,
        ...(branch.condition ? { condition: branch.condition } : {}),
        ...(branch.isDefault ? { isDefault: true } : {}),
        name: branch.name,
        nodes: branch.nodes.map(toBackendNode),
      })),
      type: `${node.branchType}_BRANCH`,
    };
  }
  return {
    ...common,
    type:
      node.type === 'copyTask'
        ? 'COPY_TASK'
        : node.type === 'handleTask'
          ? 'HANDLE_TASK'
          : 'USER_TASK',
  };
}

export function stringifyProcessModelAsset(asset: BpmProcessModelAsset): string {
  return JSON.stringify({
    schemaVersion: 2,
    nodes: asset.nodes.map(toBackendNode),
  });
}
