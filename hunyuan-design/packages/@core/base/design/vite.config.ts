// eslint-disable-next-line no-restricted-imports
import { defineConfig } from '@hunyuan/vite-config';

export default defineConfig(async () => {
  return {
    vite: {
      publicDir: 'src/scss-bem',
    },
  };
});
