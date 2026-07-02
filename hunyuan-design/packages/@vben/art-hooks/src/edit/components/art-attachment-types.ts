export type ArtAttachmentStatus = 'error' | 'ready' | 'success' | 'uploading'

export interface ArtAttachmentItem {
  category?: string
  mimeType?: string
  name: string
  percent?: number
  remark?: string
  size?: number
  status?: ArtAttachmentStatus
  thumbnailUrl?: string
  uid: string
  url?: string
}
