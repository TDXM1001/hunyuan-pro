import type { RouteRecordRaw } from 'vue-router';

import {
  HUNYUAN_DOC_URL,
  HUNYUAN_GITHUB_URL,
  HUNYUAN_LOGO_URL,
} from '@vben/constants';

import { IFrameView } from '#/layouts';
import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      badgeType: 'dot',
      icon: HUNYUAN_LOGO_URL,
      order: 9998,
      title: $t('demos.hunyuan.title'),
    },
    name: 'HunyuanProject',
    path: '/hunyuan-design',
    children: [
      {
        name: 'HunyuanDocument',
        path: '/hunyuan-design/document',
        component: IFrameView,
        meta: {
          icon: 'lucide:book-open-text',
          link: HUNYUAN_DOC_URL,
          title: $t('demos.hunyuan.document'),
        },
      },
      {
        name: 'HunyuanGithub',
        path: '/hunyuan-design/github',
        component: IFrameView,
        meta: {
          icon: 'mdi:github',
          link: HUNYUAN_GITHUB_URL,
          title: 'Github',
        },
      },
    ],
  },
  {
    name: 'HunyuanAbout',
    path: '/hunyuan-design/about',
    component: () => import('#/views/_core/about/index.vue'),
    meta: {
      icon: 'lucide:copyright',
      title: $t('demos.hunyuan.about'),
      order: 9999,
    },
  },
  {
    name: 'Profile',
    path: '/profile',
    component: () => import('#/views/_core/profile/index.vue'),
    meta: {
      icon: 'lucide:user',
      hideInMenu: true,
      title: $t('page.auth.profile'),
    },
  },
];

export default routes;
