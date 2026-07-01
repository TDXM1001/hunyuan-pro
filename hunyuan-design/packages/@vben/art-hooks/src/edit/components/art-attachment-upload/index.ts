import { defineComponent, h } from 'vue'

import { ElUpload } from 'element-plus'

import './style.css'

export default defineComponent({
  name: 'ArtAttachmentUpload',
  props: {
    acceptText: { default: '支持 PDF、Word、Excel，单个文件不超过 20MB', type: String },
    icon: { default: 'upload', type: String },
    text: { default: '点击或拖拽文件到此处上传', type: String },
  },
  setup(props, { attrs, slots }) {
    return () =>
      h(
        ElUpload,
        {
          action: '#',
          autoUpload: false,
          class: 'art-attachment-upload',
          drag: true,
          ...attrs,
        },
        {
          default: () => [
            h(
              'span',
              { class: 'art-attachment-upload__icon' },
              props.icon === 'document' ? '□' : '↑',
            ),
            h('div', { class: 'art-attachment-upload__text' }, props.text),
          ],
          tip: () =>
            slots.tip?.() || h('div', { class: 'art-attachment-upload__tip' }, props.acceptText),
        },
      )
  },
})
