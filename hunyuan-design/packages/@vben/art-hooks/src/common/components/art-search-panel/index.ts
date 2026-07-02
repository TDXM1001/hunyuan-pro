import type { PropType } from 'vue'

import { defineComponent, h } from 'vue'

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
    loading: { default: false, type: Boolean },
    refreshText: { default: '刷新', type: String },
    resetText: { default: '重置', type: String },
    searchText: { default: '搜索', type: String },
    searchType: { default: 'primary', type: String as PropType<ArtSearchPanelButtonType> },
    showRefresh: { default: true, type: Boolean },
  },
  emits: ['refresh', 'reset', 'search'],
  setup(props, { emit, slots }) {
    function handleSubmit(event: Event) {
      event.preventDefault()
      emit('search')
    }

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
            slots.default?.(),
            h(
              'div',
              { class: 'art-search-panel__actions' },
              [
                slots.actions?.(),
                h(ElSpace, { wrap: true }, () => [
                  h(
                    ElButton,
                    {
                      loading: props.loading,
                      nativeType: 'submit',
                      type: props.searchType === 'default' ? undefined : props.searchType,
                    },
                    () => props.searchText,
                  ),
                  h(ElButton, { onClick: () => emit('reset') }, () => props.resetText),
                  props.showRefresh
                    ? h(
                        ElButton,
                        {
                          plain: true,
                          type: 'success',
                          onClick: () => emit('refresh'),
                        },
                        () => props.refreshText,
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
