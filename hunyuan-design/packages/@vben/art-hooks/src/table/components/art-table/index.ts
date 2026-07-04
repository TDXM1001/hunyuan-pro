import type { ColumnOption } from '../../types'

import { computed, defineComponent, h, nextTick, ref } from 'vue'

import { useWindowSize } from '@vueuse/core'
import { VbenLoading } from '@vben-core/shadcn-ui'
import { EmptyIcon } from '@vben/icons'
import {
  ElPagination,
  ElTable,
  ElTableColumn,
} from 'element-plus'

import { useTableSettings } from '../../composables/useTableSettings'
import {
  getArtTableFullscreenState,
  useArtTableFullscreen,
} from '../../composables/useArtTableFullscreen'

import './style.css'

interface PaginationConfig {
  current: number
  size: number
  total: number
}

interface PaginationOptions {
  align?: 'center' | 'left' | 'right'
  background?: boolean
  hideOnSinglePage?: boolean
  layout?: string
  pagerCount?: number
  pageSizes?: number[]
  showPageCount?: boolean
  showTotalSummary?: boolean
  size?: 'default' | 'large' | 'small'
}

const DEFAULT_TABLE_HEIGHT = 420

const STATUS_COLUMN_PROPS = new Set(['status', 'state'])
const ACTION_COLUMN_PROPS = new Set(['action', 'actions', 'operation', 'operations'])

function normalizeColumn(col: ColumnOption): ColumnOption {
  const normalized = { ...col }

  if (!normalized.align) {
    normalized.align = normalized.type === 'selection' ? 'center' : 'left'
  }

  if (!normalized.headerAlign) {
    normalized.headerAlign = normalized.align
  }

  if (normalized.type === 'globalIndex' || normalized.type === 'index') {
    normalized.align = 'center'
    normalized.headerAlign = 'center'
    normalized.width ??= 70
  }

  if (normalized.type === 'selection') {
    normalized.align = 'center'
    normalized.headerAlign = 'center'
    normalized.width ??= 52
  }

  if (normalized.prop && STATUS_COLUMN_PROPS.has(normalized.prop)) {
    normalized.align ??= 'center'
    normalized.headerAlign ??= normalized.align
    normalized.width ??= 110
  }

  if (normalized.prop && ACTION_COLUMN_PROPS.has(normalized.prop)) {
    normalized.align ??= 'center'
    normalized.headerAlign ??= normalized.align
    normalized.fixed ??= 'right'
    normalized.width ??= 180
  }

  if (!normalized.width && !normalized.minWidth) {
    normalized.minWidth = normalized.useSlot ? 140 : 120
  }

  if (
    normalized.showOverflowTooltip === undefined &&
    normalized.type !== 'selection' &&
    normalized.type !== 'expand' &&
    normalized.type !== 'index' &&
    normalized.type !== 'globalIndex' &&
    !normalized.useSlot
  ) {
    normalized.showOverflowTooltip = true
  }

  return normalized
}

function cleanColumnProps(col: ColumnOption) {
  const columnProps = { ...col }
  delete columnProps.headerSlotName
  delete columnProps.slotName
  delete columnProps.useHeaderSlot
  delete columnProps.useSlot
  return columnProps
}

export default defineComponent({
  name: 'ArtTable',
  props: {
    border: { default: undefined, type: Boolean },
    columns: { default: () => [], type: Array as () => ColumnOption[] },
    data: { default: () => [], type: Array as () => Record<string, any>[] },
    emptyHeight: { default: '100%', type: String },
    emptyText: { default: '暂无数据', type: String },
    height: { default: undefined, type: [Number, String] },
    loading: { default: false, type: Boolean },
    pagination: { default: undefined, type: Object as () => PaginationConfig | undefined },
    paginationOptions: { default: () => ({}), type: Object as () => PaginationOptions },
    showTableHeader: { default: true, type: Boolean },
    stripe: { default: undefined, type: Boolean },
  },
  emits: ['pagination:current-change', 'pagination:size-change'],
  setup(props, { attrs, emit, slots, expose }) {
    const elTableRef = ref<any>(null)
    const artTableRef = ref<HTMLElement>()
    const { width } = useWindowSize()
    const { isBorder, isHeaderBackground, isZebra, tableSize } = useTableSettings()
    const fullscreenContext = useArtTableFullscreen()

    const isFullScreen = computed(() => {
      if (fullscreenContext) {
        return fullscreenContext.isFullScreen.value
      }
      return getArtTableFullscreenState(artTableRef.value).value
    })

    const isEmpty = computed(() => props.data.length === 0)
    const showLoadingOverlay = computed(() => props.loading)
    const resolvedTableHeight = computed(() => {
      if (isFullScreen.value) return '100%'
      return props.height ?? DEFAULT_TABLE_HEIGHT
    })
    const tableHeight = computed(() => {
      if (!isEmpty.value || props.loading) {
        return resolvedTableHeight.value
      }
      return props.emptyHeight === '100%' ? resolvedTableHeight.value : props.emptyHeight
    })

    const layout = computed(() => {
      if (width.value < 768) return 'prev, pager, next, sizes, jumper, total'
      if (width.value < 1024) return 'prev, pager, next, jumper, total'
      return 'total, prev, pager, next, sizes, jumper'
    })

    const mergedPaginationOptions = computed(() => ({
      align: 'center' as const,
      background: true,
      hideOnSinglePage: false,
      layout: layout.value,
      pageSizes: [10, 20, 30, 50, 100],
      pagerCount: width.value > 1200 ? 7 : 5,
      showPageCount: false,
      showTotalSummary: false,
      size: 'default' as const,
      ...props.paginationOptions,
    }))

    const paginationSummary = computed(() => {
      if (!props.pagination) return ''
      const totalText = `共 ${props.pagination.total} 条`
      if (!mergedPaginationOptions.value.showPageCount) {
        return totalText
      }
      const pageCount = props.pagination.total > 0
        ? Math.ceil(props.pagination.total / props.pagination.size)
        : 0
      return `${totalText} / ${pageCount} 页`
    })

    function scrollToTop() {
      nextTick(() => {
        elTableRef.value?.setScrollTop?.(0)
      })
    }

    function getGlobalIndex(index: number) {
      if (!props.pagination) return index + 1
      return (props.pagination.current - 1) * props.pagination.size + index + 1
    }

    expose({ elTableRef, scrollToTop })

    return () => {
      const tableColumns = props.columns.map((rawCol) => {
        const col = normalizeColumn(rawCol)
        if (col.type === 'globalIndex') {
          return h(
            ElTableColumn,
            { ...col, key: col.prop || col.type },
            {
              default: ({ $index }: { $index: number }) => h('span', getGlobalIndex($index)),
            },
          )
        }

        const columnSlots: Record<string, any> = {}
        if (col.useHeaderSlot && col.prop) {
          columnSlots.header = (headerScope: any) =>
            slots[col.headerSlotName || `${col.prop}-header`]?.({
              ...headerScope,
              label: col.label,
              prop: col.prop,
            }) || col.label
        }
        if (col.useSlot && col.prop) {
          columnSlots.default = (slotScope: any) => {
            if (slotScope?.$index !== undefined && slotScope.$index < 0) return null
            return slots[col.slotName || col.prop!]?.({
              ...slotScope,
              prop: col.prop,
              value: slotScope?.row?.[col.prop!],
            })
          }
        }

        return h(ElTableColumn, { ...cleanColumnProps(col), key: col.prop || col.type }, columnSlots)
      })

      const pagination = props.pagination && props.data.length > 0
        ? h(
            'div',
            {
              class: [
                'pagination',
                'custom-pagination',
                mergedPaginationOptions.value.align,
                {
                  'has-summary':
                    mergedPaginationOptions.value.showTotalSummary ||
                    mergedPaginationOptions.value.showPageCount,
                },
              ],
            },
            [
              mergedPaginationOptions.value.showTotalSummary ||
              mergedPaginationOptions.value.showPageCount
                ? h(
                    'span',
                    { class: 'pagination-summary' },
                    paginationSummary.value,
                  )
                : null,
              h(ElPagination, {
                ...mergedPaginationOptions.value,
                currentPage: props.pagination.current,
                disabled: props.loading,
                pageSize: props.pagination.size,
                total: props.pagination.total,
                onCurrentChange: (val: number) => {
                  emit('pagination:current-change', val)
                  scrollToTop()
                },
                onSizeChange: (val: number) => emit('pagination:size-change', val),
              }),
            ],
          )
        : null

      // 统一由组件层接管表格容器与分页皮肤，页面只负责业务布局。
      return h(
        'div',
        {
          ref: artTableRef,
          class: ['art-table', { 'is-empty': isEmpty.value }],
          style: { minHeight: 0, width: '100%' },
        },
        [
          h(
            ElTable,
            {
              ...attrs,
              ref: elTableRef,
              border: props.border ?? isBorder.value,
              data: props.data,
              headerCellStyle: {
                background: isHeaderBackground.value
                  ? 'var(--el-fill-color-lighter)'
                  : 'var(--el-bg-color)',
              },
              height: tableHeight.value,
              loading: false,
              size: tableSize.value,
              stripe: props.stripe ?? isZebra.value,
            },
            {
              default: () => [...tableColumns, slots.default?.()],
              empty: () =>
                props.loading
                  ? h('div')
                  : h('div', { class: 'art-table-empty' }, [
                      h(EmptyIcon, { class: 'art-table-empty__icon' }),
                      h('div', { class: 'art-table-empty__title' }, props.emptyText),
                      h(
                        'div',
                        { class: 'art-table-empty__description' },
                        '可以调整筛选条件后再试，或刷新当前列表。',
                      ),
                    ]),
            },
          ),
          showLoadingOverlay.value
            ? h(
                'div',
                { class: 'art-table-loading' },
                h(VbenLoading, {
                  spinning: true,
                  text: props.data.length > 0 ? '正在刷新列表...' : '正在加载数据...',
                }),
              )
            : null,
          pagination,
        ],
      )
    }
  },
})
