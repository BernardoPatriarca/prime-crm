export interface ReorderItem {
  id: string;
  displayOrder: number;
}

export interface ReorderRequest {
  items: ReorderItem[];
}
