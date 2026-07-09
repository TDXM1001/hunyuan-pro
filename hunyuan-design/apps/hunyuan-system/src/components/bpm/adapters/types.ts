export interface BpmListenerBinding {
  channels: string[];
  listenerCode: string;
}

export interface BpmFormDesignerSnapshot {
  layoutJson: string;
  schemaJson: string;
}

export interface BpmProcessNodeDraft {
  approvalMode?: 'single' | 'singleOnly';
  candidateResolverType?:
    | 'DEPARTMENT_MANAGER'
    | 'EMPLOYEE'
    | 'EMPLOYEE_SELECT_AT_START'
    | 'ROLE'
    | 'START_DEPARTMENT_MANAGER'
    | 'START_EMPLOYEE';
  employeeSelectFieldKey?: string;
  id: string;
  listeners: BpmListenerBinding[];
  name: string;
  nodeKey: string;
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
