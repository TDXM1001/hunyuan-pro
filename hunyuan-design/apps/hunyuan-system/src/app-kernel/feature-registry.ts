import { createAppFeatureRegistry } from '@hunyuan/app-kernel';
import { accessFeature } from '@hunyuan/feature-access';
import { identityAccountFeature } from '@hunyuan/feature-identity-account';
import { identityEmployeeFeature } from '@hunyuan/feature-identity-employee';
import {
  organizationFeature,
  organizationPositionFeature,
} from '@hunyuan/feature-organization';
import { platformConfigurationFeature } from '@hunyuan/feature-platform-configuration';
import { platformFileFeature } from '@hunyuan/feature-platform-file';

export const appFeatureRegistry = createAppFeatureRegistry([
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
]);
