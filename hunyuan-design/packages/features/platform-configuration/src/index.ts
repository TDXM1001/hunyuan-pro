import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 仅声明已完成迁移的稳定路由，历史菜单仍由应用兼容层按 component 读取。
export const platformConfigurationFeature = {
  capabilities: [
    'support:config:query',
    'support:config:add',
    'support:config:update',
    'support:dict:query',
    'support:dict:add',
    'support:dict:update',
    'support:dict:delete',
    'support:dict:updateDisabled',
    'support:dictData:query',
    'support:dictData:add',
    'support:dictData:update',
    'support:dictData:delete',
    'support:dictData:updateDisabled',
  ],
  id: 'platform.configuration',
  routes: [
    {
      path: '/config/config-list',
      routeId: 'platform.configuration.parameters',
    },
    {
      path: '/setting/dict',
      routeId: 'platform.configuration.dictionary',
    },
  ],
} as const satisfies AppFeatureDefinition;

export {
  addConfig,
  buildConfigMutationPayload,
  buildConfigPageQueryPayload,
  queryConfigPage,
  updateConfig,
} from './configuration/client';
export type {
  ConfigAddForm,
  ConfigPageQueryParams,
  ConfigRecord,
  ConfigUpdateForm,
  PageResult as ConfigurationPageResult,
} from './configuration/client';
export {
  addDict,
  addDictData,
  batchDeleteDictData,
  batchDeleteDicts,
  buildDictDataMutationPayload,
  buildDictMutationPayload,
  buildDictOptionsByCode,
  buildDictPageQueryPayload,
  deleteDict,
  deleteDictData,
  queryAllDictData,
  queryDictDataList,
  queryDictOptionsByCode,
  queryDictPage,
  toggleDictDataDisabled,
  toggleDictDisabled,
  updateDict,
  updateDictData,
} from './dictionary/client';
export type {
  DictAddForm,
  DictDataAddForm,
  DictDataRecord,
  DictDataUpdateForm,
  DictOption,
  DictPageQueryParams,
  DictRecord,
  DictUpdateForm,
  PageResult as DictionaryPageResult,
} from './dictionary/client';
export { platformConfigurationRequestClientKey } from './dependencies';
