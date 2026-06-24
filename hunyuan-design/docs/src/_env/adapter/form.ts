import type {
  HunyuanFormSchema as FormSchema,
  HunyuanFormProps,
} from '@hunyuan/common-ui';

import type { ComponentType } from './component';

import { setupHunyuanForm, useHunyuanForm as useForm, z } from '@hunyuan/common-ui';
import { $t } from '@hunyuan/locales';

import { initComponentAdapter } from './component';

initComponentAdapter();

setupHunyuanForm<ComponentType>({
  config: {
    baseModelPropName: 'value',
    // naive-ui组件的空值为null,不能是undefined，否则重置表单时不生效
    emptyStateValue: null,
    modelPropNameMap: {
      Checkbox: 'checked',
      Radio: 'checked',
      Switch: 'checked',
      Upload: 'fileList',
    },
  },
  defineRules: {
    required: (value, _params, ctx) => {
      if (value === undefined || value === null || value.length === 0) {
        return $t('ui.formRules.required', [ctx.label]);
      }
      return true;
    },
    selectRequired: (value, _params, ctx) => {
      if (value === undefined || value === null) {
        return $t('ui.formRules.selectRequired', [ctx.label]);
      }
      return true;
    },
  },
});

const useHunyuanForm = useForm<ComponentType>;

export { useHunyuanForm, z };

export type HunyuanFormSchema = FormSchema<ComponentType>;
export type { HunyuanFormProps };
