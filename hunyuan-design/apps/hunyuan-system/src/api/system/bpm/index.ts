export const BPM_ENDPOINT_INDEX = {
  category: '/bpm/category/',
  definition: '/bpm/definition/',
  designer: '/bpm/designer/',
  form: '/bpm/form/',
  integration: '/bpm/integration/',
  listener: '/bpm/listener/',
  model: '/bpm/model/',
  runtimeTask: '/bpm/task/',
  sampleExpense: '/bpm/sample/expense/',
} as const;

export * from './category';
export * from './definition';
export * from './form';
export * from './integration';
export * from './listener';
export * from './model';
export * from './runtime';
export * from './sample-expense';
