import type { SearchRequest, SearchResponse } from '../types/api';
import { ApiResponseError, post } from './apiClient';
import { fallbackSearchResponse } from './offlineFallback';

export function submitSearch(query: string): Promise<SearchResponse> {
  const body: SearchRequest = { query };
  return post<SearchResponse, SearchRequest>('/search', body).catch((error: unknown) => {
    if (error instanceof ApiResponseError && [502, 503, 504].includes(error.status)) {
      return Promise.resolve(fallbackSearchResponse(query));
    }
    if (error instanceof TypeError) {
      return Promise.resolve(fallbackSearchResponse(query));
    }
    return Promise.reject(error);
  });
}
