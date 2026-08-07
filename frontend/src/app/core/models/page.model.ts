export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}
