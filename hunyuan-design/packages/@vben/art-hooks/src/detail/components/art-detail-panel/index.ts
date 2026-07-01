import { defineComponent, h } from 'vue'

import './style.css'

export default defineComponent({
  name: 'ArtDetailPanel',
  setup(_, { slots }) {
    return () =>
      h(
        'div',
        {
          class: 'art-detail-panel',
        },
        slots.default?.(),
      )
  },
})
