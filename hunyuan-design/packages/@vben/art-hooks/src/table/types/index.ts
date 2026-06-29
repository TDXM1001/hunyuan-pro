/**
 * 表格组件类型定义
 *
 * 从 art-design-pro 提取并适配到 @vben 项目
 */

import type { TableColumnCtx } from 'element-plus'

/**
 * 表格列配置接口
 */
export interface ColumnOption<T = any> {
  // 列类型
  type?: 'selection' | 'expand' | 'index' | 'globalIndex'
  // 列属性名
  prop?: string
  // 列标题
  label?: string
  // 水平对齐
  align?: 'center' | 'left' | 'right'
  // 表头对齐
  headerAlign?: 'center' | 'left' | 'right'
  // 列宽度
  width?: string | number
  // 最小列宽度
  minWidth?: string | number
  // 固定列
  fixed?: boolean | 'left' | 'right'
  // 是否可排序
  sortable?: boolean | 'custom'
  // 内容过长时是否展示 tooltip
  showOverflowTooltip?: boolean
  // 过滤器选项
  filters?: any[]
  // 过滤方法
  filterMethod?: (value: any, row: any) => boolean
  // 过滤器位置
  filterPlacement?: string
  // 是否禁用
  disabled?: boolean
  // 是否显示列
  visible?: boolean
  // 是否选中显示
  checked?: boolean
  // 自定义渲染函数
  formatter?: (row: T) => any
  // 插槽相关配置
  // 是否使用插槽渲染内容
  useSlot?: boolean
  // 插槽名称（默认为 prop 值）
  slotName?: string
  // 是否使用表头插槽
  useHeaderSlot?: boolean
  // 表头插槽名称（默认为 `${prop}-header`）
  headerSlotName?: string
  // 其他属性
  [key: string]: any
}

/**
 * API 命名空间
 * 用于定义通用的 API 响应类型
 */
export namespace Api {
  export namespace Common {
    /** 分页参数 */
    export interface PaginationParams {
      /** 当前页码 */
      current: number
      /** 每页条数 */
      size: number
      /** 总条数 */
      total: number
    }

    /** 分页响应 */
    export interface PaginatedResponse<T = any> {
      /** 数据列表 */
      records: T[]
      /** 总条数 */
      total: number
      /** 当前页码 */
      current: number
      /** 每页条数 */
      size: number
    }
  }
}

export type { TableColumnCtx }
