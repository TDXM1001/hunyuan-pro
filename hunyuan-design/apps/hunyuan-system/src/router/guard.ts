import type { Router } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { resetAllStores, useAccessStore, useUserStore } from '@vben/stores';
import { startProgress, stopProgress } from '@vben/utils';

import { accessRoutes, coreRouteNames } from '#/router/routes';
import { useAuthStore } from '#/store';

import { generateAccess } from './access';

function setupCommonGuard(router: Router) {
  const loadedPaths = new Set<string>();

  router.beforeEach((to) => {
    to.meta.loaded = loadedPaths.has(to.path);

    if (!to.meta.loaded && preferences.transition.progress) {
      startProgress();
    }
    return true;
  });

  router.afterEach((to) => {
    loadedPaths.add(to.path);

    if (preferences.transition.progress) {
      stopProgress();
    }
  });
}

function buildLoginRedirect(fullPath: string) {
  return {
    path: LOGIN_PATH,
    query:
      fullPath === LOGIN_PATH || fullPath === preferences.app.defaultHomePath
        ? {}
        : { redirect: encodeURIComponent(fullPath) },
    replace: true,
  };
}

function setupAccessGuard(router: Router) {
  router.beforeEach(async (to, from) => {
    const accessStore = useAccessStore();
    const userStore = useUserStore();
    const authStore = useAuthStore();

    if (coreRouteNames.includes(to.name as string)) {
      return true;
    }

    if (!accessStore.accessToken) {
      if (to.meta.ignoreAccess) {
        return true;
      }

      if (to.fullPath !== LOGIN_PATH) {
        return buildLoginRedirect(to.fullPath);
      }
      return to;
    }

    if (accessStore.isAccessChecked) {
      return true;
    }

    try {
      const userInfo = userStore.userInfo || (await authStore.fetchUserInfo());
      const userRoles = userInfo.roles ?? [];

      const { accessibleMenus, accessibleRoutes } = await generateAccess({
        roles: userRoles,
        router,
        routes: accessRoutes,
      });

      accessStore.setAccessMenus(accessibleMenus);
      accessStore.setAccessRoutes(accessibleRoutes);
      accessStore.setIsAccessChecked(true);

      const redirectPath = (from.query.redirect ??
        (to.path === preferences.app.defaultHomePath
          ? userInfo.homePath || preferences.app.defaultHomePath
          : to.fullPath)) as string;

      return {
        ...router.resolve(decodeURIComponent(redirectPath)),
        replace: true,
      };
    } catch (error) {
      console.error('[hunyuan-system] access guard bootstrap failed', error);
      resetAllStores();
      return buildLoginRedirect(to.fullPath);
    }
  });
}

function createRouterGuard(router: Router) {
  setupCommonGuard(router);
  setupAccessGuard(router);
}

export { createRouterGuard };
