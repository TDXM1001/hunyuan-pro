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

export interface BpmProcessNodeDraft {
  approvalMode?: 'parallelAll' | 'sequential' | 'single' | 'singleOnly';
  candidateResolverType?:
    | 'DEPARTMENT_MANAGER'
    | 'EMPLOYEE'
    | 'EMPLOYEE_SELECT_AT_START'
    | 'ROLE'
    | 'START_DEPARTMENT_MANAGER'
    | 'START_EMPLOYEE';
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
  type: 'userTask';
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
