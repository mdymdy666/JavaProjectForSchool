import { http } from './http'
import type { ApiResponse } from '../types/domain'

export interface UploadResult {
  url: string
  filename: string
}

export async function uploadImage(file: File): Promise<ApiResponse<UploadResult>> {
  const form = new FormData()
  form.append('file', file)
  const res = await http.post<ApiResponse<UploadResult>>('/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}
