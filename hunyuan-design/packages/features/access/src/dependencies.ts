import type { InjectionKey } from 'vue';

import type { AccessClient } from './contract';

export const accessClientKey: InjectionKey<AccessClient> =
  Symbol('accessClient');
