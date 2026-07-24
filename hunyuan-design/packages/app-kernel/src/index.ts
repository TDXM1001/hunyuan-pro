export interface FeatureRouteDefinition {
  path: string;
  routeId: string;
}

export interface AppFeatureDefinition {
  capabilities?: readonly string[];
  dependencies?: readonly string[];
  id: string;
  routes: readonly FeatureRouteDefinition[];
}

export type FeatureRouteLoader = () => Promise<unknown>;

export interface AppFeatureRegistration {
  enabled?: boolean;
  feature: AppFeatureDefinition;
  routeLoaders: Readonly<Record<string, FeatureRouteLoader>>;
}

export interface AppFeatureRegistry {
  createPageMap(): Record<string, FeatureRouteLoader>;
  hasRoute(routeId: string): boolean;
  resolveComponent(routeId: string): string | undefined;
}

const COMPONENT_PREFIX = '/__app_kernel__/';

function componentKey(routeId: string) {
  return `${COMPONENT_PREFIX}${routeId}`;
}

/**
 * 创建应用唯一模块注册表，并在启动阶段阻断不完整或冲突的模块声明。
 */
export function createAppFeatureRegistry(
  registrations: readonly AppFeatureRegistration[],
): AppFeatureRegistry {
  const enabledRegistrations = registrations.filter(
    (registration) => registration.enabled !== false,
  );
  const featureIds = new Set<string>();
  const routeLoaders = new Map<string, FeatureRouteLoader>();

  for (const { feature } of enabledRegistrations) {
    if (featureIds.has(feature.id)) {
      throw new Error(`模块 ID 重复：${feature.id}`);
    }
    featureIds.add(feature.id);
  }

  for (const {
    feature,
    routeLoaders: featureLoaders,
  } of enabledRegistrations) {
    for (const dependency of feature.dependencies ?? []) {
      if (!featureIds.has(dependency)) {
        throw new Error(`模块 ${feature.id} 缺少依赖：${dependency}`);
      }
    }

    const declaredRouteIds = new Set(
      feature.routes.map((route) => route.routeId),
    );
    for (const route of feature.routes) {
      const loader = featureLoaders[route.routeId];
      if (!loader) {
        throw new Error(`模块 ${feature.id} 缺少路由加载器：${route.routeId}`);
      }
      if (routeLoaders.has(route.routeId)) {
        throw new Error(`routeId 冲突：${route.routeId}`);
      }
      routeLoaders.set(route.routeId, loader);
    }

    for (const routeId of Object.keys(featureLoaders)) {
      if (!declaredRouteIds.has(routeId)) {
        throw new Error(
          `模块 ${feature.id} 注册了未声明的 routeId：${routeId}`,
        );
      }
    }
  }

  return {
    createPageMap() {
      return Object.fromEntries(
        [...routeLoaders].map(([routeId, loader]) => [
          `${componentKey(routeId)}.vue`,
          loader,
        ]),
      );
    },
    hasRoute(routeId) {
      return routeLoaders.has(routeId);
    },
    resolveComponent(routeId) {
      return routeLoaders.has(routeId) ? componentKey(routeId) : undefined;
    },
  };
}
