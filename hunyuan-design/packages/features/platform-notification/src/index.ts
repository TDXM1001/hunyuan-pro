import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

export const platformNotificationFeature = {
  capabilities: [
    'support:message:query', 'support:message:add', 'support:message:delete',
    'support:sms:template:query', 'support:sms:template:add',
    'support:sms:template:update', 'support:sms:sendLog:query',
  ],
  id: 'platform.notification',
  routes: [
    { routeId: 'platform.notification.message' },
    { routeId: 'platform.notification.sms-template' },
    { routeId: 'platform.notification.sms-send-log' },
  ],
} as const satisfies AppFeatureDefinition;

export { platformNotificationRequestClientKey } from './dependencies';
