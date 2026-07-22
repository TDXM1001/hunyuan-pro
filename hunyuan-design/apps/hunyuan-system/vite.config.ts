import { defineConfig } from '@vben/vite-config';

import ElementPlus from 'unplugin-element-plus/vite';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      plugins: [
        ElementPlus({
          format: 'esm',
        }),
      ],
      resolve: {
        // 工作区源包统一复用同一份 Vue 与 Element Plus，避免开发态出现多个运行时实例。
        dedupe: ['vue', 'element-plus'],
      },
      server: {
        proxy: {
          '/api/admin/v1': {
            changeOrigin: true,
            target: 'http://localhost:1024',
            ws: true,
          },
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            target: 'http://localhost:1024',
            ws: true,
          },
        },
      },
    },
  };
});
