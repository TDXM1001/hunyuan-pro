import { createAppFeatureRegistry } from '@hunyuan/app-kernel';
import { accessFeature } from '@hunyuan/feature-access';
import { foundationAcceptanceFeature } from '@hunyuan/feature-foundation-acceptance';
import { identityAccountFeature } from '@hunyuan/feature-identity-account';
import { identityEmployeeFeature } from '@hunyuan/feature-identity-employee';
import {
  organizationFeature,
  organizationPositionFeature,
} from '@hunyuan/feature-organization';
import { platformConfigurationFeature } from '@hunyuan/feature-platform-configuration';
import { platformAuditFeature } from '@hunyuan/feature-platform-audit';
import { platformDevtoolsFeature } from '@hunyuan/feature-platform-devtools';
import { platformFileFeature } from '@hunyuan/feature-platform-file';
import { platformNotificationFeature } from '@hunyuan/feature-platform-notification';
import { platformRuntimeFeature } from '@hunyuan/feature-platform-runtime';
import { platformSecurityFeature } from '@hunyuan/feature-platform-security';

export const appFeatureRegistry = createAppFeatureRegistry([
  {
    feature: foundationAcceptanceFeature,
    routeLoaders: {
      'foundation.acceptance.probe': () =>
        import('@hunyuan/feature-foundation-acceptance/page'),
    },
  },
  {
    feature: identityAccountFeature,
    routeLoaders: {},
  },
  {
    feature: organizationFeature,
    routeLoaders: {
      'organization.department.directory': () =>
        import('#/views/organization/directory/index.vue'),
    },
  },
  {
    feature: organizationPositionFeature,
    routeLoaders: {
      'organization.position.directory': () =>
        import('#/views/system/position/position-list.vue'),
    },
  },
  {
    feature: identityEmployeeFeature,
    routeLoaders: {
      'identity.employee.management': () =>
        import('#/views/system/employee/index.vue'),
    },
  },
  {
    feature: accessFeature,
    routeLoaders: {
      'access.menu.management': () =>
        import('#/views/system/menu/menu-list.vue'),
      'access.role.management': () => import('#/views/system/role/index.vue'),
    },
  },
  {
    feature: platformConfigurationFeature,
    routeLoaders: {
      'platform.configuration.dictionary': () =>
        import('#/feature-entries/platform-configuration/dictionary-page.vue'),
      'platform.configuration.parameters': () =>
        import('#/feature-entries/platform-configuration/configuration-page.vue'),
    },
  },
  {
    feature: platformFileFeature,
    routeLoaders: {
      'platform.file.management': () =>
        import('#/feature-entries/platform-file/management-page.vue'),
    },
  },
  {
    feature: platformAuditFeature,
    routeLoaders: {
      'platform.audit.login-log': () => import('#/feature-entries/platform-audit/login-log-page.vue'),
      'platform.audit.operation-log': () => import('#/feature-entries/platform-audit/operation-log-page.vue'),
    },
  },
  {
    feature: platformNotificationFeature,
    routeLoaders: {
      'platform.notification.message': () => import('#/feature-entries/platform-notification/message-page.vue'),
      'platform.notification.sms-template': () => import('#/feature-entries/platform-notification/sms-template-page.vue'),
      'platform.notification.sms-send-log': () => import('#/feature-entries/platform-notification/sms-send-log-page.vue'),
    },
  },
  {
    feature: platformSecurityFeature,
    routeLoaders: {
      'platform.security.baseline-settings': () => import('#/feature-entries/platform-security/settings-page.vue'),
      'platform.security.login-failure': () => import('#/feature-entries/platform-security/login-failure-page.vue'),
      'platform.security.data-masking-validation': () => import('#/feature-entries/platform-security/data-masking-page.vue'),
    },
  },
  {
    feature: platformRuntimeFeature,
    routeLoaders: {
      'platform.runtime.job': () => import('#/feature-entries/platform-runtime/job-page.vue'),
      'platform.runtime.serial-number': () => import('#/feature-entries/platform-runtime/serial-number-page.vue'),
      'platform.runtime.cache': () => import('#/feature-entries/platform-runtime/cache-page.vue'),
      'platform.runtime.reload': () => import('#/feature-entries/platform-runtime/reload-page.vue'),
    },
  },
  {
    feature: platformDevtoolsFeature,
    routeLoaders: {
      'platform.devtools.api-encrypt': () => import('#/feature-entries/platform-devtools/api-encrypt-page.vue'),
    },
  },
]);
