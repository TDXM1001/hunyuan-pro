export interface FeatureRouteDefinition {
  /** URL path 由后端授权菜单持有，feature 只声明稳定装配标识。 */
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

function assertAcyclicDependencies(
  registrations: readonly AppFeatureRegistration[],
) {
  const dependencyMap = new Map(
    registrations.map(({ feature }) => [
      feature.id,
      feature.dependencies ?? [],
    ]),
  );
  const visiting = new Set<string>();
  const visited = new Set<string>();

  function visit(featureId: string, path: string[]) {
    if (visiting.has(featureId)) {
      const cycleStart = path.indexOf(featureId);
      const cycle = [...path.slice(cycleStart), featureId];
      throw new Error(`模块依赖循环：${cycle.join(' -> ')}`);
    }
    if (visited.has(featureId)) {
      return;
    }

    visiting.add(featureId);
    for (const dependency of dependencyMap.get(featureId) ?? []) {
      visit(dependency, [...path, featureId]);
    }
    visiting.delete(featureId);
    visited.add(featureId);
  }

  for (const featureId of dependencyMap.keys()) {
    visit(featureId, []);
  }
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
  const capabilityOwners = new Map<string, string>();
  const routeLoaders = new Map<string, FeatureRouteLoader>();

  for (const { feature } of enabledRegistrations) {
    if (featureIds.has(feature.id)) {
      throw new Error(`模块 ID 重复：${feature.id}`);
    }
    featureIds.add(feature.id);

    for (const capability of feature.capabilities ?? []) {
      const owner = capabilityOwners.get(capability);
      if (owner) {
        throw new Error(
          `能力码冲突：${capability}（${owner} / ${feature.id}）`,
        );
      }
      capabilityOwners.set(capability, feature.id);
    }
  }

  for (const { feature } of enabledRegistrations) {
    for (const dependency of feature.dependencies ?? []) {
      if (!featureIds.has(dependency)) {
        throw new Error(`模块 ${feature.id} 缺少依赖：${dependency}`);
      }
    }
  }
  // 依赖存在并不代表可装配；环会让初始化顺序和能力注入失去确定性，因此在应用启动前拒绝。
  assertAcyclicDependencies(enabledRegistrations);

  for (const {
    feature,
    routeLoaders: featureLoaders,
  } of enabledRegistrations) {
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
