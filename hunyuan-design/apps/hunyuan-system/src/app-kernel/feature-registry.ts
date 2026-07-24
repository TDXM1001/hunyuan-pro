import { createAppFeatureRegistry } from '@hunyuan/app-kernel';
import { accessFeature } from '@hunyuan/feature-access';
import { identityEmployeeFeature } from '@hunyuan/feature-identity-employee';
import {
  organizationFeature,
  organizationPositionFeature,
} from '@hunyuan/feature-organization';

export const appFeatureRegistry = createAppFeatureRegistry([
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
]);
