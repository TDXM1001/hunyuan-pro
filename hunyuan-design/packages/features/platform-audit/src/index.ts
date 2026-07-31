import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 审计 feature 只注册只读日志路由，具体查询条件和详情字段由各自客户端契约维护。
export const platformAuditFeature = {
  capabilities: [
    'support:loginLog:query',
    'support:operateLog:query',
    'support:operateLog:detail',
  ],
  id: 'platform.audit',
  routes: [
    { routeId: 'platform.audit.login-log' },
    { routeId: 'platform.audit.operation-log' },
  ],
} as const satisfies AppFeatureDefinition;

export { platformAuditRequestClientKey } from './dependencies';
