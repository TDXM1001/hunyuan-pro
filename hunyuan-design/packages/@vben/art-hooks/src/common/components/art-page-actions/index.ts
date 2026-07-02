import type { ArtActionButtonSize, ArtActionItem } from '../art-action-group'
import type { PropType } from 'vue'

import { defineComponent, h } from 'vue'

import ArtActionGroup from '../art-action-group'

import './style.css'

export default defineComponent({
  name: 'ArtPageActions',
  props: {
    actions: { default: () => [], type: Array as PropType<ArtActionItem[]> },
    size: { default: 'default', type: String as PropType<ArtActionButtonSize> },
    wrap: { default: true, type: Boolean },
  },
  setup(props) {
    return () =>
      h('div', { class: 'art-page-actions' }, [
        h(ArtActionGroup, {
          actions: props.actions,
          link: false,
          size: props.size,
          wrap: props.wrap,
        }),
      ])
  },
})
