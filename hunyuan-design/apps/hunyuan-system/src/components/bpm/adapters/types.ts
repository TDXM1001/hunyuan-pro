export interface BpmListenerBinding {
  channels: string[];
  listenerCode: string;
}

export type BpmFieldPermissionMode = 'EDITABLE' | 'HIDDEN' | 'READONLY';

export interface BpmNodeFieldPermission {
  fieldKey: string;
  permission: BpmFieldPermissionMode;
  required: boolean;
}

export interface BpmFormDesignerSnapshot {
  layoutJson: string;
  schemaJson: string;
}

export type BpmCandidateResolverType =
  | 'DEPARTMENT_MANAGER'
  | 'EMPLOYEE'
  | 'EMPLOYEE_SELECT_AT_START'
  | 'ROLE'
  | 'START_DEPARTMENT_MANAGER'
  | 'START_EMPLOYEE';

export interface BpmProcessNodeBaseDraft {
  approvalMode?: 'parallelAll' | 'sequential' | 'single' | 'singleOnly';
  candidateResolverType?: BpmCandidateResolverType;
  departmentId?: number;
  employeeId?: number;
  employeeIds?: number[];
  employeeSelectFieldKey?: string;
  fieldPermissions?: BpmNodeFieldPermission[];
  id: string;
  listeners: BpmListenerBinding[];
  name: string;
  nodeKey: string;
  roleId?: number;
}

export interface BpmHumanTaskNodeDraft extends BpmProcessNodeBaseDraft {
  type: 'handleTask' | 'userTask';
}

export interface BpmCopyTaskNodeDraft extends BpmProcessNodeBaseDraft {
  type: 'copyTask';
}

export interface BpmRouteConditionDraft {
  [key: string]: unknown;
}

export interface BpmProcessBranchDraft {
  branchKey: string;
  condition?: BpmRouteConditionDraft;
  isDefault?: boolean;
  name: string;
  nodes: BpmProcessNodeDraft[];
}

export interface BpmBranchNodeDraft extends BpmProcessNodeBaseDraft {
  branches: BpmProcessBranchDraft[];
  branchType: 'EXCLUSIVE' | 'INCLUSIVE' | 'PARALLEL';
  type: 'branch';
}

export type BpmProcessNodeDraft =
  | BpmBranchNodeDraft
  | BpmCopyTaskNodeDraft
  | BpmHumanTaskNodeDraft;

export interface BpmProcessModelAsset {
  nodes: BpmProcessNodeDraft[];
  schemaVersion: 2;
}

export interface BpmProcessDesignerSnapshot {
  bpmnXml: string;
  nodes: BpmProcessNodeDraft[];
}

export interface BpmFormDesignerExpose {
  getSnapshot: () => BpmFormDesignerSnapshot;
  isDirty: () => boolean;
  load: (snapshot: Partial<BpmFormDesignerSnapshot>) => Promise<void>;
  resetDirty: () => void;
  validate: () => Promise<{ message?: string; ok: boolean }>;
}

export interface BpmProcessDesignerExpose {
  getSnapshot: () => BpmProcessDesignerSnapshot;
  isDirty: () => boolean;
  load: (snapshot: Partial<BpmProcessDesignerSnapshot>) => Promise<void>;
  resetDirty: () => void;
  validate: () => Promise<{ message?: string; ok: boolean }>;
}
