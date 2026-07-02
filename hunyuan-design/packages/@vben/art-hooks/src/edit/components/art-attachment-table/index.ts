import type { PropType } from 'vue'
import type { ArtAttachmentItem, ArtAttachmentStatus } from '../art-attachment-types'
import type { ColumnOption } from '../../../table/types'

import { defineComponent, h } from 'vue'

import {
  ElButton,
  ElInput,
  ElSpace,
  ElTag,
  ElUpload,
} from 'element-plus'

import { ArtTable } from '../../../table/hooks'

import './style.css'

const statusMap: Record<ArtAttachmentStatus, { text: string; type: 'danger' | 'info' | 'success' | 'warning' }> = {
  error: { text: '失败', type: 'danger' },
  ready: { text: '待上传', type: 'info' },
  success: { text: '已上传', type: 'success' },
  uploading: { text: '上传中', type: 'warning' },
}

function createFileItem(file: any): ArtAttachmentItem {
  const raw = file?.raw
  return {
    mimeType: raw?.type,
    name: file?.name || '未命名文件',
    size: raw?.size,
    status: 'ready',
    uid: file?.uid ? String(file.uid) : String(Date.now()),
  }
}

function formatFileSize(size?: number) {
  if (!size) return '-'
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

export default defineComponent({
  name: 'ArtAttachmentTable',
  props: {
    accept: { default: '', type: String },
    emptyText: { default: '暂无附件', type: String },
    height: { default: 260, type: [Number, String] },
    modelValue: { default: () => [], type: Array as PropType<ArtAttachmentItem[]> },
    readonly: { default: false, type: Boolean },
    uploadText: { default: '上传附件', type: String },
  },
  emits: ['download', 'remove', 'update:modelValue'],
  setup(props, { emit }) {
    const columns: ColumnOption<ArtAttachmentItem>[] = [
      { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
      { prop: 'name', label: '文件名', minWidth: 220, useSlot: true },
      { prop: 'category', label: '分类', width: 120 },
      { prop: 'size', label: '大小', width: 100, align: 'center', useSlot: true },
      { prop: 'status', label: '状态', width: 110, align: 'center', useSlot: true },
      { prop: 'remark', label: '备注', minWidth: 180, useSlot: true },
      { prop: 'operation', label: '操作', width: props.readonly ? 100 : 150, fixed: 'right', useSlot: true },
    ]

    function updateList(nextList: ArtAttachmentItem[]) {
      emit('update:modelValue', nextList)
    }

    function handleChange(file: any) {
      if (props.readonly) return
      updateList([...props.modelValue, createFileItem(file)])
    }

    function handleRemove(item: ArtAttachmentItem) {
      updateList(props.modelValue.filter((target) => target.uid !== item.uid))
      emit('remove', item)
    }

    function updateRemark(item: ArtAttachmentItem, remark: string) {
      updateList(props.modelValue.map((target) =>
        target.uid === item.uid ? { ...target, remark } : target,
      ))
    }

    return () =>
      h('div', { class: 'art-attachment-table' }, [
        props.readonly
          ? null
          : h('div', { class: 'art-attachment-table__toolbar' }, [
              h(
                ElUpload,
                {
                  accept: props.accept,
                  action: '#',
                  autoUpload: false,
                  showFileList: false,
                  onChange: handleChange,
                },
                {
                  default: () =>
                    h(ElButton, { type: 'primary', plain: true }, () => props.uploadText),
                },
              ),
            ]),
        h(
          ArtTable,
          {
            columns,
            data: props.modelValue,
            emptyText: props.emptyText,
            height: props.height,
          },
          {
            name: ({ row }: { row: ArtAttachmentItem }) =>
              h('div', { class: 'art-attachment-table__name' }, [
                h('span', { class: 'art-attachment-table__file-name' }, row.name),
                row.mimeType
                  ? h('span', { class: 'art-attachment-table__mime' }, row.mimeType)
                  : null,
              ]),
            operation: ({ row }: { row: ArtAttachmentItem }) =>
              h(ElSpace, { size: 'small' }, () => [
                h(ElButton, {
                  link: true,
                  type: 'primary',
                  onClick: () => emit('download', row),
                }, () => '下载'),
                props.readonly
                  ? null
                  : h(ElButton, {
                      link: true,
                      type: 'danger',
                      onClick: () => handleRemove(row),
                    }, () => '删除'),
              ]),
            remark: ({ row }: { row: ArtAttachmentItem }) =>
              props.readonly
                ? row.remark || '-'
                : h(ElInput, {
                    modelValue: row.remark,
                    placeholder: '请输入备注',
                    size: 'small',
                    onUpdateModelValue: (value: string) => updateRemark(row, value),
                  }),
            size: ({ row }: { row: ArtAttachmentItem }) => formatFileSize(row.size),
            status: ({ row }: { row: ArtAttachmentItem }) => {
              const status = row.status || 'ready'
              const statusConfig = statusMap[status]
              return h(ElTag, { effect: 'light', type: statusConfig.type }, () => statusConfig.text)
            },
          },
        ),
      ])
  },
})
