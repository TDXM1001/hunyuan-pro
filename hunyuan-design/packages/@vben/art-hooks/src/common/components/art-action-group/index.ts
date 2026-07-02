import type { Component, PropType } from 'vue'

import { defineComponent, h } from 'vue'

import {
  ElButton,
  ElMessageBox,
  ElSpace,
} from 'element-plus'

export type ArtActionButtonType =
  | 'danger'
  | 'default'
  | 'info'
  | 'primary'
  | 'success'
  | 'warning'

export type ArtActionButtonSize = 'default' | 'large' | 'small'

export interface ArtActionConfirm {
  cancelButtonText?: string
  confirmButtonText?: string
  message: string
  title?: string
  type?: 'error' | 'info' | 'success' | 'warning'
}

export interface ArtActionItem {
  confirm?: ArtActionConfirm | string
  disabled?: boolean
  icon?: Component
  key: string
  label: string
  link?: boolean
  loading?: boolean
  onClick?: () => Promise<void> | void
  size?: ArtActionButtonSize
  type?: ArtActionButtonType
}

function normalizeConfirm(confirm: ArtActionConfirm | string): ArtActionConfirm {
  if (typeof confirm === 'string') {
    return { message: confirm }
  }
  return confirm
}

export default defineComponent({
  name: 'ArtActionGroup',
  props: {
    actions: { default: () => [], type: Array as PropType<ArtActionItem[]> },
    link: { default: true, type: Boolean },
    size: { default: 'small', type: String as PropType<ArtActionButtonSize> },
    wrap: { default: true, type: Boolean },
  },
  setup(props) {
    async function handleActionClick(action: ArtActionItem) {
      if (action.disabled || action.loading) return

      if (action.confirm) {
        const confirm = normalizeConfirm(action.confirm)
        try {
          await ElMessageBox.confirm(confirm.message, confirm.title || '确认操作', {
            cancelButtonText: confirm.cancelButtonText || '取消',
            confirmButtonText: confirm.confirmButtonText || '确定',
            type: confirm.type || 'warning',
          })
        } catch {
          return
        }
      }

      await action.onClick?.()
    }

    return () =>
      h(
        ElSpace,
        { size: 'small', wrap: props.wrap },
        () =>
          props.actions.map((action) =>
            h(
              ElButton,
              {
                key: action.key,
                disabled: action.disabled,
                link: action.link ?? props.link,
                loading: action.loading,
                icon: action.icon,
                size: action.size || props.size,
                type: action.type === 'default' ? undefined : action.type,
                onClick: () => handleActionClick(action),
              },
              () => action.label,
            ),
          ),
      )
  },
})
