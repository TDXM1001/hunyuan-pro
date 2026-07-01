import type { VNodeChild } from 'vue'

export type DetailItemAlign = 'center' | 'left' | 'right'

export type DetailValue = boolean | null | number | string | undefined

export interface DetailItem<T = Record<string, any>> {
  align?: DetailItemAlign
  class?: string
  emptyText?: string
  formatter?: (value: any, data: T, item: DetailItem<T>) => VNodeChild
  label: string
  labelClass?: string
  prop?: keyof T | string
  slotName?: string
  span?: number
  useSlot?: boolean
  value?: DetailValue | ((data: T) => DetailValue)
  valueClass?: string
}

export interface DetailSection<T = Record<string, any>> {
  class?: string
  columns?: number
  description?: string
  extraSlotName?: string
  items: DetailItem<T>[]
  key: string
  title: string
}
