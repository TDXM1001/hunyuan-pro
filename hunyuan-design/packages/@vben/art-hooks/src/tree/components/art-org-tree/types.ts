import type Node from 'element-plus/es/components/tree/src/model/node';

export interface OrgTreeNode {
  id: number | string;
  label: string;
  parentId?: null | number | string;
  children?: OrgTreeNode[];
  count?: number;
  disabled?: boolean;
  [key: string]: any;
}

export interface OrgTreeProps {
  data: OrgTreeNode[];
  showCount?: boolean;
  defaultExpandAll?: boolean;
  highlightCurrent?: boolean;
  nodeKey?: string;
  currentNodeKey?: null | number | string;
  emptyText?: string;
  filterNodeMethod?: (value: string, data: OrgTreeNode, node: Node) => boolean;
}

export interface OrgTreeEmits {
  (e: 'node-click', data: OrgTreeNode, node: Node): void;
  (e: 'current-change', data: null | OrgTreeNode, node: null | Node): void;
  (e: 'update:currentNodeKey', key: null | number | string): void;
}

export interface OrgTreeSlots {
  default?: (scope: { node: Node; data: OrgTreeNode }) => any;
  empty?: () => any;
}
