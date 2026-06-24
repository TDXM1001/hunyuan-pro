import type {
  BaseFormComponentType,
  ExtendedFormApi,
  HunyuanFormProps,
} from './types';

import { defineComponent, h, isReactive, onBeforeUnmount, watch } from 'vue';

import { useSelector } from '@hunyuan-core/shared/store';

import { FormApi } from './form-api';
import HunyuanUseForm from './hunyuan-use-form.vue';

export function useHunyuanForm<
  T extends BaseFormComponentType = BaseFormComponentType,
  P extends Record<string, any> = Record<never, never>,
>(options: HunyuanFormProps<T, P>) {
  const IS_REACTIVE = isReactive(options);
  const api = new FormApi(options as unknown as HunyuanFormProps);
  const extendedApi: ExtendedFormApi = api as never;
  extendedApi.useStore = (selector) => {
    return useSelector(api.store, selector);
  };

  const Form = defineComponent(
    (props: HunyuanFormProps, { attrs, slots }) => {
      onBeforeUnmount(() => {
        api.unmount();
      });
      api.setState({ ...props, ...attrs });
      return () =>
        h(HunyuanUseForm, { ...props, ...attrs, formApi: extendedApi }, slots);
    },
    {
      name: 'HunyuanUseForm',
      inheritAttrs: false,
    },
  );
  // Add reactivity support
  if (IS_REACTIVE) {
    watch(
      () => options.schema,
      () => {
        api.setState({ schema: options.schema });
      },
      { immediate: true },
    );
  }

  return [Form, extendedApi] as const;
}
