import type { PropType } from 'vue'
import type { ArtAttachmentItem } from '../art-attachment-types'

import { defineComponent, h } from 'vue'

import {
  ElButton,
  ElImage,
  ElUpload,
} from 'element-plus'

import './style.css'

function createImageItem(file: any): ArtAttachmentItem {
  const raw = file?.raw
  const url = raw && typeof URL !== 'undefined' ? URL.createObjectURL(raw) : undefined
  return {
    mimeType: raw?.type,
    name: file?.name || '未命名图片',
    size: raw?.size,
    status: 'ready',
    thumbnailUrl: url,
    uid: file?.uid ? String(file.uid) : String(Date.now()),
    url,
  }
}

export default defineComponent({
  name: 'ArtImageUpload',
  props: {
    accept: { default: 'image/*', type: String },
    max: { default: 6, type: Number },
    modelValue: { default: () => [], type: Array as PropType<ArtAttachmentItem[]> },
    readonly: { default: false, type: Boolean },
    text: { default: '上传图片', type: String },
  },
  emits: ['preview', 'remove', 'update:modelValue'],
  setup(props, { emit }) {
    function updateList(nextList: ArtAttachmentItem[]) {
      emit('update:modelValue', nextList)
    }

    function handleChange(file: any) {
      if (props.readonly || props.modelValue.length >= props.max) return
      updateList([...props.modelValue, createImageItem(file)])
    }

    function handleRemove(item: ArtAttachmentItem) {
      const nextList = props.modelValue.filter((target) => target.uid !== item.uid)
      updateList(nextList)
      emit('remove', item)
    }

    return () =>
      h('div', { class: 'art-image-upload' }, [
        h(
          'div',
          { class: 'art-image-upload__grid' },
          [
            ...props.modelValue.map((item) => {
              const imageUrl = item.thumbnailUrl || item.url
              return h('div', { class: 'art-image-upload__item', key: item.uid }, [
                imageUrl
                  ? h(ElImage, {
                      class: 'art-image-upload__image',
                      fit: 'cover',
                      previewSrcList: [imageUrl],
                      src: imageUrl,
                    })
                  : h('div', { class: 'art-image-upload__placeholder' }, item.name),
                h('div', { class: 'art-image-upload__mask' }, [
                  h(ElButton, {
                    circle: true,
                    size: 'small',
                    type: 'primary',
                    onClick: () => emit('preview', item),
                  }, () => '看'),
                  props.readonly
                    ? null
                    : h(ElButton, {
                        circle: true,
                        size: 'small',
                        type: 'danger',
                        onClick: () => handleRemove(item),
                      }, () => '删'),
                ]),
              ])
            }),
            props.readonly || props.modelValue.length >= props.max
              ? null
              : h(
                  ElUpload,
                  {
                    accept: props.accept,
                    action: '#',
                    autoUpload: false,
                    class: 'art-image-upload__trigger',
                    drag: false,
                    showFileList: false,
                    onChange: handleChange,
                  },
                  {
                    default: () => [
                      h('span', { class: 'art-image-upload__trigger-icon' }, '+'),
                      h('span', props.text),
                    ],
                  },
                ),
          ],
        ),
      ])
  },
})
