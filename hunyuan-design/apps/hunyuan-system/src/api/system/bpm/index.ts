export const BPM_ENDPOINT_INDEX = {
  category: '/bpm/category/',
  definition: '/bpm/definition/',
  designer: '/bpm/designer/',
  form: '/bpm/form/',
  graphDraft: '/bpm/graph-draft/',
  graphDefinition: '/bpm/graph-definition/',
  integration: '/bpm/integration/',
  timeEvent: '/bpm/time-event/',
  connector: '/bpm/connector/',
  listener: '/bpm/listener/',
  model: '/bpm/model/',
  operations: '/bpm/operations/',
  evolution: '/bpm/evolution/',
  policyCatalog: '/bpm/policy-catalog/',
  businessContract: '/bpm/business-contract/',
  runtimeTask: '/bpm/task/',
  sampleExpense: '/bpm/sample/expense/',
} as const;

export * from './category';
export * from './business-contract';
export * from './definition';
export * from './evolution';
export * from './form';
export * from './graph';
export * from './integration';
export * from './listener';
export * from './model';
export * from './operations';
export * from './policy';
export * from './runtime';
export * from './sample-expense';
export * from './time-event';
