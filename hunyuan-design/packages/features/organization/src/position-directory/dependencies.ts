import type { InjectionKey } from 'vue';

import type { OrganizationPositionClient } from './contract';

export const organizationPositionClientKey: InjectionKey<OrganizationPositionClient> =
  Symbol('organizationPositionClient');
