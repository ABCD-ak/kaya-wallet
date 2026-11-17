import request from '@/utils/request'
import type { Category, PageResult } from './types'

export interface ListCategoriesParams {
  type?: string
  page?: number
  pageSize?: number
}

export function listCategories(params?: ListCategoriesParams): Promise<PageResult<Category>> {
  const query: Record<string, unknown> = { pageSize: params?.pageSize ?? 100 }
  if (params?.type) query.type = params.type
  if (params?.page) query.page = params.page
  return request.get<PageResult<Category>>('/categories', query)
}
