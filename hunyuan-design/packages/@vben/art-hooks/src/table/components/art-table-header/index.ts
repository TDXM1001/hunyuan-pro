import type { ColumnOption } from '../../types'

import { computed, defineComponent, h, onMounted, onUnmounted, ref, watch } from 'vue'

import {
  ElButton,
  ElCheckbox,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElPopover,
  ElScrollbar,
  ElTag,
} from 'element-plus'

import { type TableSize, useTableSettings } from '../../composables/useTableSettings'
import {
  createFallbackFullscreenController,
  resolveArtTableFullscreenTarget,
  useArtTableFullscreen,
} from '../../composables/useArtTableFullscreen'

import './style.css'

interface ColumnStateSnapshot {
  checked?: boolean
  visible?: boolean
}

export default defineComponent({
  name: 'ArtTableHeader',
  props: {
    layout: {
      default: 'search,refresh,size,fullscreen,columns,settings',
      type: String,
    },
    loading: { default: false, type: Boolean },
    modelValue: { default: () => [], type: Array as () => ColumnOption[] },
    showBorder: { default: true, type: Boolean },
    showHeaderBackground: { default: true, type: Boolean },
    showSearchBar: { default: undefined, type: Boolean },
    showZebra: { default: true, type: Boolean },
  },
  emits: ['refresh', 'search', 'update:modelValue', 'update:showSearchBar'],
  setup(props, { emit, slots }) {
    const isManualRefresh = ref(false)
    const headerRef = ref<HTMLElement>()
    const draggingIndex = ref<null | number>(null)
    const dropIndex = ref<null | number>(null)
    const initialColumns = ref<ColumnOption[]>([])
    const initialColumnStates = ref<Record<string, ColumnStateSnapshot>>({})

    const {
      isBorder,
      isHeaderBackground,
      isZebra,
      setIsBorder,
      setIsHeaderBackground,
      setIsZebra,
      setTableSize,
      tableSize,
    } = useTableSettings()

    const fullscreenContext = useArtTableFullscreen()
    let fallbackFullscreen = createFallbackFullscreenController(null)

    const isFullScreen = computed(() => {
      if (fullscreenContext) {
        return fullscreenContext.isFullScreen.value
      }
      return fallbackFullscreen?.isFullScreen.value ?? false
    })

    const canUseFullscreen = computed(() => {
      if (fullscreenContext) return true
      return Boolean(resolveArtTableFullscreenTarget(headerRef.value))
    })

    const toggleFullScreen = () => {
      if (fullscreenContext) {
        fullscreenContext.toggleFullScreen()
        return
      }
      fallbackFullscreen?.toggleFullScreen()
    }

    const tableSizeOptions: Array<{ label: string; value: TableSize }> = [
      { label: '紧凑', value: 'small' },
      { label: '默认', value: 'default' },
      { label: '宽松', value: 'large' },
    ]

    const layoutItems = computed(() => props.layout.split(',').map((item) => item.trim()))
    const shouldShow = (componentName: string) => layoutItems.value.includes(componentName)

    const getColumnKey = (col: ColumnOption) => `${col.prop || col.type || col.label || 'column'}`

    const getColumnVisibility = (col: ColumnOption) => col.visible ?? col.checked ?? true

    // 记录首次渲染的列顺序，支持“一键恢复默认”。
    const captureInitialColumns = () => {
      if (initialColumns.value.length > 0 || props.modelValue.length === 0) {
        return
      }

      initialColumns.value = props.modelValue.map((item) => ({ ...item }))
      initialColumnStates.value = Object.fromEntries(
        props.modelValue.map((item) => [
          getColumnKey(item),
          {
            checked: item.checked,
            visible: item.visible,
          },
        ]),
      )
    }

    const visibleColumnCount = computed(() => {
      return props.modelValue.filter((item) => getColumnVisibility(item)).length
    })

    const allColumnsVisible = computed(() => {
      return props.modelValue.every((item) => item.disabled || getColumnVisibility(item))
    })

    const canResetColumns = computed(() => {
      if (!initialColumns.value.length || props.modelValue.length !== initialColumns.value.length) {
        return true
      }

      return props.modelValue.some((item, index) => {
        const key = getColumnKey(item)
        const defaultKey = getColumnKey(initialColumns.value[index] || item)
        const defaultState = initialColumnStates.value[key]

        return (
          key !== defaultKey ||
          getColumnVisibility(item) !== (defaultState?.visible ?? defaultState?.checked ?? true)
        )
      })
    })

    const updateColumnVisibility = (
      col: ColumnOption,
      value: boolean | number | string,
    ) => {
      const visible = Boolean(value)
      col.checked = visible
      col.visible = visible
    }

    const setAllColumnsVisible = (visible: boolean) => {
      props.modelValue.forEach((item) => {
        if (!item.disabled) {
          updateColumnVisibility(item, visible)
        }
      })
    }

    const reorderColumns = (fromIndex: number, toIndex: number) => {
      if (
        fromIndex === toIndex ||
        fromIndex < 0 ||
        toIndex < 0 ||
        fromIndex >= props.modelValue.length ||
        toIndex >= props.modelValue.length
      ) {
        return
      }

      const source = props.modelValue[fromIndex]
      const target = props.modelValue[toIndex]
      if (!source || !target || source.fixed || source.disabled || target.fixed || target.disabled) {
        return
      }

      const next = [...props.modelValue]
      const [current] = next.splice(fromIndex, 1)
      if (!current) return
      next.splice(toIndex, 0, current)
      emit('update:modelValue', next)
    }

    const resetColumns = () => {
      const next = initialColumns.value.map((item) => {
        const key = getColumnKey(item)
        const state = initialColumnStates.value[key]
        return {
          ...item,
          checked: state?.checked ?? item.checked,
          visible: state?.visible ?? item.visible ?? true,
        }
      })
      emit('update:modelValue', next)
    }

    const search = () => {
      emit('update:showSearchBar', !props.showSearchBar)
      emit('search')
    }

    const refresh = () => {
      isManualRefresh.value = true
      emit('refresh')
    }

    const handleToolButtonKeydown = (event: KeyboardEvent, onClick?: () => void) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        onClick?.()
      }
    }

    const handleTableSizeChange = (command: TableSize) => {
      setTableSize(command)
    }

    const handleDragStart = (index: number, event: DragEvent) => {
      const current = props.modelValue[index]
      if (!current || current.fixed || current.disabled) {
        event.preventDefault()
        return
      }

      draggingIndex.value = index
      dropIndex.value = index
      if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move'
        event.dataTransfer.setData('text/plain', String(index))
      }
    }

    const handleDragOver = (index: number, event: DragEvent) => {
      if (draggingIndex.value === null) return
      event.preventDefault()
      dropIndex.value = index
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
      }
    }

    const handleDragEnd = () => {
      draggingIndex.value = null
      dropIndex.value = null
    }

    const handleDrop = (index: number, event: DragEvent) => {
      event.preventDefault()
      if (draggingIndex.value === null) return
      reorderColumns(draggingIndex.value, index)
      handleDragEnd()
    }

    watch(
      () => props.modelValue,
      () => {
        captureInitialColumns()
      },
      { deep: true, immediate: true },
    )

    // 手动刷新只在本次加载周期内高亮，避免工具栏一直停留在 loading 态。
    watch(
      () => props.loading,
      (loading) => {
        if (!loading) {
          isManualRefresh.value = false
        }
      },
      { immediate: true },
    )

    // 按钮使用统一的几何图形字符，减少字体差异带来的观感跳动。
    const renderToolButton = (
      iconText: string,
      options: {
        active?: boolean
        compact?: boolean
        disabled?: boolean
        loading?: boolean
        onClick?: () => void
        title: string
      },
    ) => {
      return h(
        'div',
        {
          'aria-disabled': options.disabled ? 'true' : undefined,
          'aria-label': options.title,
          class: [
            'button',
            {
              active: options.active,
              compact: options.compact,
              disabled: options.disabled,
              loading: options.loading,
            },
          ],
          onClick: options.disabled ? undefined : options.onClick,
          onKeydown: (event: KeyboardEvent) =>
            options.disabled ? undefined : handleToolButtonKeydown(event, options.onClick),
          role: 'button',
          tabindex: options.disabled ? -1 : 0,
          title: options.title,
        },
        [h('span', { class: 'button-icon' }, iconText)],
      )
    }

    onMounted(() => {
      if (!fullscreenContext) {
        fallbackFullscreen = createFallbackFullscreenController(headerRef.value)
      }
    })

    onUnmounted(() => {
      fallbackFullscreen?.exitFullScreen()
    })

    return () =>
      h('div', { id: 'art-table-header', ref: headerRef, class: 'art-table-header' }, [
        h('div', { class: 'header-left' }, slots.left?.()),
        h('div', { class: 'header-right' }, [
          props.showSearchBar != null && shouldShow('search')
            ? renderToolButton('⌕', {
                active: props.showSearchBar,
                onClick: search,
                title: '搜索栏开关',
              })
            : null,
          shouldShow('refresh')
            ? renderToolButton('↻', {
                loading: props.loading && isManualRefresh.value,
                onClick: refresh,
                title: '刷新',
              })
            : null,
          shouldShow('size')
            ? h(
                ElDropdown,
                { onCommand: handleTableSizeChange },
                {
                  default: () => renderToolButton('▥', { title: '表格尺寸' }),
                  dropdown: () =>
                    h(
                      ElDropdownMenu,
                      {},
                      () =>
                        tableSizeOptions.map((item) =>
                          h(
                            ElDropdownItem,
                            {
                              key: item.value,
                              command: item.value,
                              class: [
                                'table-size-btn-item',
                                { 'is-active': tableSize.value === item.value },
                              ],
                            },
                            {
                              default: () =>
                                h('div', { class: 'dropdown-item-content' }, [
                                  h('span', item.label),
                                  tableSize.value === item.value
                                    ? h(
                                        ElTag,
                                        { effect: 'plain', size: 'small', type: 'primary' },
                                        { default: () => '当前' },
                                      )
                                    : null,
                                ]),
                            },
                          ),
                        ),
                    ),
                },
              )
            : null,
          shouldShow('fullscreen')
            ? renderToolButton(isFullScreen.value ? '⤡' : '⤢', {
                active: isFullScreen.value,
                disabled: !canUseFullscreen.value,
                onClick: toggleFullScreen,
                title: canUseFullscreen.value
                  ? isFullScreen.value
                    ? '退出全屏 (Esc)'
                    : '表格全屏'
                  : '请使用 ArtTablePanel 包裹表格区域',
              })
            : null,
          shouldShow('columns')
            ? h(
                ElPopover,
                {
                  placement: 'bottom-end',
                  popperClass: 'art-table-popover',
                  trigger: 'click',
                  width: 260,
                },
                {
                  reference: () => renderToolButton('☰', { title: '列设置' }),
                  default: () =>
                    h('div', { class: 'column-panel' }, [
                      h('div', { class: 'panel-header' }, [
                        h('div', { class: 'panel-title' }, '列设置'),
                        h('div', { class: 'panel-actions' }, [
                          h(
                            ElButton,
                            {
                              disabled: allColumnsVisible.value,
                              link: true,
                              size: 'small',
                              type: 'primary',
                              onClick: () => setAllColumnsVisible(true),
                            },
                            { default: () => '全选' },
                          ),
                          h(
                            ElButton,
                            {
                              disabled: visibleColumnCount.value === 0,
                              link: true,
                              size: 'small',
                              onClick: () => setAllColumnsVisible(false),
                            },
                            { default: () => '全不选' },
                          ),
                          h(
                            ElButton,
                            {
                              disabled: !canResetColumns.value,
                              link: true,
                              size: 'small',
                              onClick: resetColumns,
                            },
                            { default: () => '重置' },
                          ),
                        ]),
                      ]),
                      h(
                        'div',
                        { class: 'panel-meta' },
                        `${visibleColumnCount.value} / ${props.modelValue.length} 列可见`,
                      ),
                      h(
                        ElScrollbar,
                        { maxHeight: '280px' },
                        () =>
                          h(
                            'div',
                            { class: 'column-list' },
                            props.modelValue.map((item, index) => {
                              const visible = getColumnVisibility(item)
                              const isDragging = draggingIndex.value === index
                              const isDropTarget = dropIndex.value === index
                              const label =
                                item.label ||
                                (item.type === 'selection' ? '选择列' : '未命名列')

                              return h(
                                'div',
                                {
                                  key: getColumnKey(item),
                                  class: [
                                    'column-option',
                                    {
                                      'fixed-column': Boolean(item.fixed),
                                      'is-dragging': isDragging,
                                      'is-drop-target': isDropTarget,
                                      'is-hidden': !visible,
                                    },
                                  ],
                                  draggable: !item.fixed && !item.disabled,
                                  onDragend: handleDragEnd,
                                  onDragover: (event: DragEvent) => handleDragOver(index, event),
                                  onDragstart: (event: DragEvent) => handleDragStart(index, event),
                                  onDrop: (event: DragEvent) => handleDrop(index, event),
                                },
                                [
                                  h('span', {
                                    class: [
                                      'drag-handle',
                                      { disabled: item.fixed || item.disabled },
                                    ],
                                    title: item.fixed ? '固定列不可拖拽' : '拖拽排序',
                                  }),
                                  h(
                                    ElCheckbox,
                                    {
                                      class: 'column-checkbox',
                                      disabled: item.disabled,
                                      modelValue: visible,
                                      'onUpdate:modelValue': (
                                        val: boolean | number | string,
                                      ) => updateColumnVisibility(item, val),
                                    },
                                    {
                                      default: () =>
                                        h('span', { class: 'column-label' }, label),
                                    },
                                  ),
                                  Boolean(item.fixed)
                                    ? h(
                                        ElTag,
                                        {
                                          class: 'fixed-tag',
                                          effect: 'plain',
                                          size: 'small',
                                          type: 'info',
                                        },
                                        { default: () => '固定' },
                                      )
                                    : null,
                                ],
                              )
                            }),
                          ),
                      ),
                    ]),
                },
              )
            : null,
          shouldShow('settings')
            ? h(
                ElPopover,
                {
                  placement: 'bottom-end',
                  popperClass: 'art-table-popover',
                  trigger: 'click',
                  width: 220,
                },
                {
                  reference: () => renderToolButton('⚙', { title: '表格设置' }),
                  default: () =>
                    h('div', { class: 'settings-panel' }, [
                      h('div', { class: 'panel-title' }, '显示设置'),
                      h('div', { class: 'settings-list' }, [
                        props.showZebra
                          ? h(
                              ElCheckbox,
                              {
                                modelValue: isZebra.value,
                                'onUpdate:modelValue': (
                                  val: boolean | number | string,
                                ) => setIsZebra(Boolean(val)),
                              },
                              { default: () => '斑马纹' },
                            )
                          : null,
                        props.showBorder
                          ? h(
                              ElCheckbox,
                              {
                                modelValue: isBorder.value,
                                'onUpdate:modelValue': (
                                  val: boolean | number | string,
                                ) => setIsBorder(Boolean(val)),
                              },
                              { default: () => '边框' },
                            )
                          : null,
                        props.showHeaderBackground
                          ? h(
                              ElCheckbox,
                              {
                                modelValue: isHeaderBackground.value,
                                'onUpdate:modelValue': (
                                  val: boolean | number | string,
                                ) => setIsHeaderBackground(Boolean(val)),
                              },
                              { default: () => '表头背景' },
                            )
                          : null,
                      ]),
                    ]),
                },
              )
            : null,
          slots.right?.(),
        ]),
      ])
  },
})
