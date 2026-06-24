import type {
  HunyuanFormProps as FormProps,
  HunyuanFormSchema as FormSchema,
} from '@hunyuan/common-ui';

import type { ComponentPropsMap, ComponentType } from './component';

import { setupHunyuanForm, useHunyuanForm as useForm, z } from '@hunyuan/common-ui';
import { $t } from '@hunyuan/locales';

async function initSetupHunyuanForm() {
  setupHunyuanForm<ComponentType>({
    config: {
      modelPropNameMap: {
        Upload: 'fileList',
        CheckboxGroup: 'model-value',
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
}

const useHunyuanForm = useForm<ComponentType, ComponentPropsMap>;

export { initSetupHunyuanForm, useHunyuanForm, z };

export type HunyuanFormSchema = FormSchema<ComponentType, ComponentPropsMap>;
export type HunyuanFormProps = FormProps<ComponentType, ComponentPropsMap>;
