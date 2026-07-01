import type { PropType } from 'vue'
import type { DetailItem, DetailSection } from '../../types'

import { computed, defineComponent, h } from 'vue'

import './style.css'

function getValue<T extends Record<string, any>>(data: T, item: DetailItem<T>) {
  if (typeof item.value === 'function') {
    return item.value(data)
  }
  if (item.value !== undefined) {
    return item.value
  }
  if (!item.prop) {
    return undefined
  }
  return data[item.prop as keyof T]
}

function isEmptyValue(value: unknown) {
  return value === '' || value === null || value === undefined
}

export default defineComponent({
  name: 'ArtDetail',
  props: {
    columns: { default: 3, type: Number },
    data: { default: () => ({}), type: Object as PropType<Record<string, any>> },
    emptyText: { default: '-', type: String },
    labelWidth: { default: 112, type: [Number, String] },
    sections: { default: () => [], type: Array as PropType<Array<DetailSection<any>>> },
  },
  setup(props, { slots }) {
    const resolvedLabelWidth = computed(() =>
      typeof props.labelWidth === 'number' ? `${props.labelWidth}px` : props.labelWidth,
    )

    function renderItem(item: DetailItem, sectionColumns: number) {
      const value = getValue(props.data, item)
      const span = Math.min(Math.max(item.span ?? 1, 1), sectionColumns)
      const slotName = item.slotName || String(item.prop || item.label)

      const content = item.useSlot
        ? slots[slotName]?.({
            data: props.data,
            item,
            value,
          })
        : item.formatter
          ? item.formatter(value, props.data, item)
          : isEmptyValue(value)
            ? item.emptyText ?? props.emptyText
            : value

      return h(
        'div',
        {
          class: ['art-detail-item', item.class],
          style: {
            gridColumn: `span ${span}`,
            textAlign: item.align,
          },
        },
        [
          h(
            'div',
            {
              class: ['art-detail-item__label', item.labelClass],
              style: {
                width: resolvedLabelWidth.value,
              },
            },
            item.label,
          ),
          h(
            'div',
            {
              class: ['art-detail-item__value', item.valueClass],
            },
            content,
          ),
        ],
      )
    }

    function renderSection(section: DetailSection, index: number) {
      const sectionColumns = section.columns ?? props.columns

      return h(
        'section',
        {
          class: ['art-detail-section', section.class],
          key: section.key,
        },
        [
          h('div', { class: 'art-detail-section__header' }, [
            h('div', { class: 'art-detail-section__title-wrap' }, [
              h('span', { class: 'art-detail-section__index' }, index + 1),
              h('div', [
                h('div', { class: 'art-detail-section__title' }, section.title),
                section.description
                  ? h('div', { class: 'art-detail-section__description' }, section.description)
                  : null,
              ]),
            ]),
            section.extraSlotName && slots[section.extraSlotName]
              ? h('div', { class: 'art-detail-section__extra' }, slots[section.extraSlotName]?.({
                  section,
                }))
              : null,
          ]),
          h(
            'div',
            {
              class: 'art-detail-section__body',
              style: {
                gridTemplateColumns: `repeat(${sectionColumns}, minmax(0, 1fr))`,
              },
            },
            section.items.map((item) => renderItem(item, sectionColumns)),
          ),
        ],
      )
    }

    return () =>
      h(
        'div',
        {
          class: 'art-detail',
        },
        props.sections.map(renderSection),
      )
  },
})
