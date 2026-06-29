import { defineComponent, h, onMounted, ref } from 'vue'

import { provideArtTableFullscreen } from '../../composables/useArtTableFullscreen'

import './style.css'

export default defineComponent({
  name: 'ArtTablePanel',
  setup(_, { slots }) {
    const panelRef = ref<HTMLElement>()
    const { isFullScreen, registerTarget } = provideArtTableFullscreen()

    onMounted(() => {
      registerTarget(panelRef.value ?? null)
    })

    return () =>
      h(
        'div',
        {
          ref: panelRef,
          class: ['art-table-panel', { 'art-table-is-fullscreen': isFullScreen.value }],
          'data-art-table-fullscreen': '',
        },
        slots.default?.(),
      )
  },
})
