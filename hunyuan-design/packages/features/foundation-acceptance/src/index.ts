import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

/** 仅用于证明 feature 可通过公开协议接入，不承担生产业务或菜单。 */
export const foundationAcceptanceFeature = {
  capabilities: [],
  id: 'foundation.acceptance',
  routes: [
    {
      routeId: 'foundation.acceptance.probe',
    },
  ],
} as const satisfies AppFeatureDefinition;
