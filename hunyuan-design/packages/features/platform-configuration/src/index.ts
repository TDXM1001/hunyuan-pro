import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// feature 只声明稳定 routeId；授权菜单继续作为 URL path 的唯一事实源。
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
      routeId: 'platform.configuration.parameters',
    },
    {
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
