import { defineComponent, h } from 'vue'

import './style.css'

export default defineComponent({
  name: 'ArtEditPage',
  props: {
    description: { default: '', type: String },
    title: { required: true, type: String },
  },
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'art-edit-page' }, [
        h('div', { class: 'art-edit-page__header' }, [
          h('div', { class: 'art-edit-page__main' }, [
            slots.back?.() ? h('div', { class: 'art-edit-page__back' }, slots.back()) : null,
            h('div', { class: 'art-edit-page__title-wrap' }, [
              h('h2', { class: 'art-edit-page__title' }, props.title),
              props.description
                ? h('div', { class: 'art-edit-page__description' }, props.description)
                : null,
            ]),
          ]),
          slots.extra?.() || slots.actions?.()
            ? h('div', { class: 'art-edit-page__toolbar' }, [
                slots.extra?.()
                  ? h('div', { class: 'art-edit-page__extra' }, slots.extra())
                  : null,
                slots.actions?.()
                  ? h('div', { class: 'art-edit-page__actions' }, slots.actions())
                  : null,
              ])
            : null,
        ]),
        h('div', { class: 'art-edit-page__body' }, slots.default?.()),
        slots.footer?.()
          ? h('div', { class: 'art-edit-page__footer' }, slots.footer())
          : null,
      ])
  },
})
