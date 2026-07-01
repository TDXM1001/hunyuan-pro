import { defineComponent, h } from 'vue'

import './style.css'

export default defineComponent({
  name: 'ArtEditSection',
  props: {
    columns: { default: 3, type: Number },
    description: { default: '', type: String },
    index: { default: undefined, type: Number },
    title: { required: true, type: String },
  },
  setup(props, { slots }) {
    return () =>
      h('section', { class: 'art-edit-section' }, [
        h('div', { class: 'art-edit-section__header' }, [
          h('div', { class: 'art-edit-section__title-wrap' }, [
            props.index === undefined
              ? null
              : h('span', { class: 'art-edit-section__index' }, props.index),
            h('div', [
              h('div', { class: 'art-edit-section__title' }, props.title),
              props.description
                ? h('div', { class: 'art-edit-section__description' }, props.description)
                : null,
            ]),
          ]),
          slots.extra?.() ? h('div', { class: 'art-edit-section__extra' }, slots.extra()) : null,
        ]),
        h(
          'div',
          {
            class: 'art-edit-section__body',
            style: {
              gridTemplateColumns: `repeat(${props.columns}, minmax(0, 1fr))`,
            },
          },
          slots.default?.(),
        ),
      ])
  },
})
