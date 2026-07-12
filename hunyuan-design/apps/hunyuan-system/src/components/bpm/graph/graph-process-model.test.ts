import { describe, expect, it } from 'vitest';
import { reactive } from 'vue';

import {
  autoLayoutGraph,
  cloneGraphDocument,
  diffGraphSemantics,
  semanticFingerprint,
  simulateGraph,
  updateGraphApprovalPolicy,
  updateGraphBusinessContract,
  updateGraphCandidatePolicy,
  updateGraphEdgeRouteCondition,
  updateGraphStartVisibilityPolicy,
  type ProcessDefinitionGraph,
} from './graph-process-model';

function graph(): ProcessDefinitionGraph {
  return {
    schemaVersion: 1,
    rootScopeId: 'scope_root',
    scopes: [{ scopeId: 'scope_root', name: 'Main process' }],
    nodes: [
      { nodeId: 'start', scopeId: 'scope_root', type: 'START', name: 'Start' },
      {
        nodeId: 'review',
        scopeId: 'scope_root',
        type: 'APPROVAL',
        name: 'Finance review',
        properties: {
          approvalPolicy: { policyKey: 'finance-completion', policyVersion: 2 },
          candidatePolicy: { policyKey: 'finance-review', policyVersion: 3 },
        },
      },
      { nodeId: 'end', scopeId: 'scope_root', type: 'END', name: 'End' },
    ],
    edges: [
      { edgeId: 'edge_start_review', scopeId: 'scope_root', sourceNodeId: 'start', targetNodeId: 'review' },
      { edgeId: 'edge_review_end', scopeId: 'scope_root', sourceNodeId: 'review', targetNodeId: 'end' },
    ],
    policies: {
      businessContract: { contractKey: 'expense', contractVersion: 2 },
      startVisibilityPolicy: { policyKey: 'expense-start', policyVersion: 1 },
    },
  };
}

describe('graph process model', () => {
  it('defaults typed route-condition fields when the editor configures a non-default branch', () => {
    const source = graph();
    const configured = updateGraphEdgeRouteCondition(source, 'edge_start_review', {
      fieldKey: 'amount',
      compareValue: '5000',
    });

    expect(configured.edges[0]?.properties?.routeCondition).toEqual({
      compareValue: '5000',
      fieldKey: 'amount',
      operator: 'EQ',
      sourceType: 'FORM_FIELD',
      valueType: 'NUMBER',
    });
  });

  it('defaults missing reference versions when the editor sets all typed policy keys', () => {
    const source = graph();
    source.policies = {};
    source.nodes[1]!.properties = {};

    const withContract = updateGraphBusinessContract(source, {
      contractKey: 'm1_acceptance_contract',
    });
    const configured = updateGraphCandidatePolicy(withContract, 'review', {
      policyKey: 'm1_acceptance_policy',
    });
    const withApproval = updateGraphApprovalPolicy(configured, 'review', {
      policyKey: 'm2_completion_policy',
    });
    const completed = updateGraphStartVisibilityPolicy(withApproval, {
      policyKey: 'm2_start_visibility_policy',
    });

    expect(configured.policies.businessContract).toEqual({
      contractKey: 'm1_acceptance_contract',
      contractVersion: 1,
    });
    expect(completed.nodes.find((node) => node.nodeId === 'review')?.properties?.candidatePolicy).toEqual({
      policyKey: 'm1_acceptance_policy',
      policyVersion: 1,
    });
    expect(completed.nodes.find((node) => node.nodeId === 'review')?.properties?.approvalPolicy).toEqual({
      policyKey: 'm2_completion_policy',
      policyVersion: 1,
    });
    expect(completed.policies.startVisibilityPolicy).toEqual({
      policyKey: 'm2_start_visibility_policy',
      policyVersion: 1,
    });
    expect(simulateGraph(completed).pass).toBe(true);
  });

  it('clones a reactive graph document for an independent editor baseline', () => {
    const source = reactive(graph());
    const copy = cloneGraphDocument(source);

    expect(copy).toEqual(graph());
    expect(copy).not.toBe(source);
    expect(copy.nodes).not.toBe(source.nodes);

    copy.nodes[0]!.name = 'Changed start';
    expect(source.nodes[0]!.name).toBe('Start');
  });

  it('lays out a connected graph deterministically without changing its semantic fingerprint', () => {
    const source = graph();
    const laidOut = autoLayoutGraph(source);

    expect(laidOut.nodes.map((node) => node.layout)).toEqual([
      { x: 64, y: 80 },
      { x: 304, y: 80 },
      { x: 544, y: 80 },
    ]);
    expect(semanticFingerprint(laidOut)).toBe(semanticFingerprint(source));
  });

  it('reports publish-blocking dependency diagnostics at the offending graph element', () => {
    const source = graph();
    source.policies = {};
    source.nodes[1]!.properties = {};

    expect(simulateGraph(source).findings).toEqual([
      expect.objectContaining({ elementId: 'review', path: 'approvalPolicy', severity: 'BLOCKING' }),
      expect.objectContaining({ elementId: 'review', path: 'candidatePolicy', severity: 'BLOCKING' }),
      expect.objectContaining({ elementId: 'scope_root', path: 'businessContract', severity: 'BLOCKING' }),
      expect.objectContaining({ elementId: 'scope_root', path: 'startVisibilityPolicy', severity: 'BLOCKING' }),
    ]);
  });

  it('reports a missing default branch on a conditional split at the authored gateway', () => {
    const source = graph();
    source.nodes = [
      { nodeId: 'start', scopeId: 'scope_root', type: 'START', name: 'Start' },
      {
        nodeId: 'route_split',
        scopeId: 'scope_root',
        type: 'CONDITION',
        name: 'Amount route',
        properties: { gatewayMode: 'SPLIT', pairedGatewayId: 'route_join' },
      },
      source.nodes[1]!,
      {
        nodeId: 'route_join',
        scopeId: 'scope_root',
        type: 'CONDITION',
        name: 'Amount join',
        properties: { gatewayMode: 'JOIN', pairedGatewayId: 'route_split' },
      },
      { nodeId: 'end', scopeId: 'scope_root', type: 'END', name: 'End' },
    ];
    source.edges = [
      { edgeId: 'edge_start_route', scopeId: 'scope_root', sourceNodeId: 'start', targetNodeId: 'route_split' },
      {
        edgeId: 'edge_large',
        scopeId: 'scope_root',
        sourceNodeId: 'route_split',
        targetNodeId: 'review',
        sourcePort: 'large_amount',
        properties: {
          routeCondition: { sourceType: 'FORM_FIELD', fieldKey: 'amount', valueType: 'NUMBER', operator: 'GT', compareValue: 5000 },
        },
      },
      {
        edgeId: 'edge_small',
        scopeId: 'scope_root',
        sourceNodeId: 'route_split',
        targetNodeId: 'route_join',
        sourcePort: 'small_amount',
        properties: {
          routeCondition: { sourceType: 'FORM_FIELD', fieldKey: 'amount', valueType: 'NUMBER', operator: 'LTE', compareValue: 5000 },
        },
      },
      { edgeId: 'edge_review_join', scopeId: 'scope_root', sourceNodeId: 'review', targetNodeId: 'route_join' },
      { edgeId: 'edge_join_end', scopeId: 'scope_root', sourceNodeId: 'route_join', targetNodeId: 'end' },
    ];

    expect(simulateGraph(source).findings).toEqual(expect.arrayContaining([
      expect.objectContaining({ elementId: 'route_split', path: 'defaultRoute', severity: 'BLOCKING' }),
    ]));
  });

  it('ignores layout-only changes but exposes changed node properties in semantic diff', () => {
    const baseline = graph();
    const layoutOnly = autoLayoutGraph(graph());
    const changed = graph();
    changed.nodes[1]!.properties = {
      candidatePolicy: { policyKey: 'finance-review', policyVersion: 4 },
    };

    expect(diffGraphSemantics(baseline, layoutOnly).changedElements).toEqual([]);
    expect(diffGraphSemantics(baseline, changed).changedElements).toEqual([
      expect.objectContaining({ elementId: 'review', kind: 'NODE' }),
    ]);
  });
});
