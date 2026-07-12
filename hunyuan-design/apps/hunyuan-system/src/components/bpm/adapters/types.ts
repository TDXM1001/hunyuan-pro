export interface BpmFormDesignerSnapshot {
  layoutJson: string;
  schemaJson: string;
}

export interface BpmFormDesignerExpose {
  getSnapshot: () => BpmFormDesignerSnapshot;
  isDirty: () => boolean;
  load: (snapshot: Partial<BpmFormDesignerSnapshot>) => Promise<void>;
  resetDirty: () => void;
  validate: () => Promise<{ message?: string; ok: boolean }>;
}
