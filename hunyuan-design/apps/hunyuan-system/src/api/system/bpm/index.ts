export const BPM_ENDPOINT_INDEX = {
  category: '/bpm/category/',
  form: '/bpm/form/',
  graphDraft: '/bpm/graph-draft/',
  graphDefinition: '/bpm/graph-definition/',
  integration: '/bpm/integration/',
  timeEvent: '/bpm/time-event/',
  connector: '/bpm/connector/',
  listener: '/bpm/listener/',
  operations: '/bpm/operations/',
  evolution: '/bpm/evolution/',
  policyCatalog: '/bpm/policy-catalog/',
  businessContract: '/bpm/business-contract/',
  runtimeTask: '/bpm/task/',
} as const;

export * from './category';
export * from './business-contract';
export * from './evolution';
export * from './form';
export * from './graph';
export * from './integration';
export * from './listener';
export * from './operations';
export * from './policy';
export * from './runtime';
export * from './time-event';
