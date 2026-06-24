import type { Component } from 'vue';

export interface HunyuanPluginsFormOptions {
  useHunyuanForm: (...args: any[]) => any;
}

export interface HunyuanPluginsModalOptions {
  useHunyuanModal?: () => any;
}

export interface HunyuanPluginsMessageOptions {
  useMessage?: () => any;
}

export interface HunyuanPluginsComponentsOptions {
  [key: string]: Component;
}

export interface HunyuanPluginsOptions {
  form?: HunyuanPluginsFormOptions;
  modal?: HunyuanPluginsModalOptions;
  message?: HunyuanPluginsMessageOptions;
  components?: HunyuanPluginsComponentsOptions;
}
