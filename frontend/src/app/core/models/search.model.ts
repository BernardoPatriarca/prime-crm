export type SearchResultType = 'CUSTOMER' | 'CONTACT' | 'LEAD' | 'OPPORTUNITY' | 'TASK';

export interface SearchResult {
  type: SearchResultType;
  id: string;
  code: string | null;
  title: string;
  subtitle: string | null;
  link: string;
}

export interface GlobalSearch {
  query: string;
  total: number;
  results: SearchResult[];
}
