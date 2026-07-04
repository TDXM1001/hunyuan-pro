import type { PropType } from 'vue'

import { defineComponent, h, nextTick, onMounted, onUnmounted, ref } from 'vue'

import {
  ElButton,
  ElForm,
  ElSpace,
} from 'element-plus'

import './style.css'

export type ArtSearchPanelButtonType =
  | 'danger'
  | 'default'
  | 'info'
  | 'primary'
  | 'success'
  | 'warning'

export default defineComponent({
  name: 'ArtSearchPanel',
  props: {
    collapsible: { default: true, type: Boolean },
    defaultExpanded: { default: false, type: Boolean },
    expandText: { default: '展开', type: String },
    collapseText: { default: '收起', type: String },
    loading: { default: false, type: Boolean },
    refreshText: { default: '刷新', type: String },
    resetText: { default: '重置', type: String },
    searchText: { default: '搜索', type: String },
    searchType: { default: 'primary', type: String as PropType<ArtSearchPanelButtonType> },
    showRefresh: { default: true, type: Boolean },
  },
  emits: ['refresh', 'reset', 'search'],
  setup(props, { emit, slots }) {
    const canToggle = ref(false)
    const expanded = ref(props.defaultExpanded)
    const fieldsRef = ref<HTMLElement>()
    let resizeObserver: ResizeObserver | undefined

    function handleSubmit(event: Event) {
      event.preventDefault()
      emit('search')
    }

    function updateToggleState() {
      const fields = fieldsRef.value
      if (!fields) {
        canToggle.value = false
        return
      }

      canToggle.value = fields.scrollHeight > 34
    }

    function handleToggle() {
      expanded.value = !expanded.value
      void nextTick(updateToggleState)
    }

    onMounted(() => {
      void nextTick(() => {
        updateToggleState()
        if (typeof ResizeObserver === 'undefined' || !fieldsRef.value) {
          return
        }
        resizeObserver = new ResizeObserver(() => updateToggleState())
        resizeObserver.observe(fieldsRef.value)
      })
    })

    onUnmounted(() => {
      resizeObserver?.disconnect()
    })

    return () =>
      h('div', { class: 'art-search-panel' }, [
        h(
          ElForm,
          {
            class: 'art-search-panel__form',
            inline: true,
            onSubmit: handleSubmit,
          },
          () => [
            h(
              'div',
              {
                ref: fieldsRef,
                class: [
                  'art-search-panel__fields',
                  {
                    'is-collapsed': props.collapsible && canToggle.value && !expanded.value,
                  },
                ],
              },
              slots.default?.(),
            ),
            h(
              'div',
              { class: 'art-search-panel__actions' },
              [
                slots.actions?.(),
                h(ElSpace, { wrap: true }, () => [
                  h(
                    ElButton,
                    {
                      class: 'art-search-panel__button art-search-panel__button--search',
                      loading: props.loading,
                      nativeType: 'submit',
                      type: props.searchType === 'default' ? undefined : props.searchType,
                    },
                    () => props.searchText,
                  ),
                  h(
                    ElButton,
                    {
                      class: 'art-search-panel__button art-search-panel__button--reset',
                      onClick: () => emit('reset'),
                    },
                    () => props.resetText,
                  ),
                  props.showRefresh
                    ? h(
                        ElButton,
                        {
                          class: 'art-search-panel__button art-search-panel__button--refresh',
                          plain: true,
                          type: 'success',
                          onClick: () => emit('refresh'),
                        },
                        () => props.refreshText,
                      )
                    : null,
                  props.collapsible && canToggle.value
                    ? h(
                        ElButton,
                        {
                          class: 'art-search-panel__button art-search-panel__button--toggle',
                          onClick: handleToggle,
                        },
                        () => (expanded.value ? props.collapseText : props.expandText),
                      )
                    : null,
                ]),
              ],
            ),
          ],
        ),
      ])
  },
})
