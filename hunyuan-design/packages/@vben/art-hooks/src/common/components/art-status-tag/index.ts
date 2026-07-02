import type { PropType } from 'vue'

import { computed, defineComponent, h } from 'vue'

import { ElTag } from 'element-plus'

export type ArtStatusTagType = 'danger' | 'info' | 'primary' | 'success' | 'warning'
export type ArtStatusTagEffect = 'dark' | 'light' | 'plain'
export type ArtStatusTagSize = 'default' | 'large' | 'small'
export type ArtStatusTagValue = boolean | number | string | null | undefined

export interface ArtStatusTagOption {
  effect?: ArtStatusTagEffect
  text: string
  type?: ArtStatusTagType
}

export type ArtStatusTagMap = Record<string, ArtStatusTagOption>

function normalizeStatusKey(value: ArtStatusTagValue) {
  if (value === null || value === undefined || value === '') {
    return ''
  }
  return String(value)
}

export default defineComponent({
  name: 'ArtStatusTag',
  props: {
    effect: { default: 'light', type: String as PropType<ArtStatusTagEffect> },
    emptyText: { default: '-', type: String },
    fallbackType: { default: 'info', type: String as PropType<ArtStatusTagType> },
    map: {
      default: () => ({}),
      type: Object as PropType<ArtStatusTagMap>,
    },
    round: { default: false, type: Boolean },
    size: { default: 'small', type: String as PropType<ArtStatusTagSize> },
    value: {
      default: undefined,
      type: [Boolean, Number, String] as PropType<ArtStatusTagValue>,
    },
  },
  setup(props) {
    const statusConfig = computed(() => {
      const key = normalizeStatusKey(props.value)
      if (!key) {
        return {
          effect: props.effect,
          text: props.emptyText,
          type: props.fallbackType,
        }
      }

      return props.map[key] ?? {
        effect: props.effect,
        text: key,
        type: props.fallbackType,
      }
    })

    return () => {
      const config = statusConfig.value
      return h(
        ElTag,
        {
          effect: config.effect ?? props.effect,
          round: props.round,
          size: props.size,
          type: config.type ?? props.fallbackType,
        },
        () => config.text,
      )
    }
  },
})
