import type { RankingMode, SuggestionResponse } from '../types/api';
import { ApiResponseError, get } from './apiClient';
import { fallbackSuggestions } from './offlineFallback';

export function fetchSuggestions(
  query: string,
  ranking: RankingMode,
  signal?: AbortSignal,
): Promise<SuggestionResponse> {
  return get<SuggestionResponse>('/suggest', { q: query, ranking }, { signal }).catch((error: unknown) => {
    if (error instanceof ApiResponseError && [502, 503, 504].includes(error.status)) {
      return Promise.resolve(fallbackSuggestions(query, ranking));
    }
    if (error instanceof TypeError) {
      return Promise.resolve(fallbackSuggestions(query, ranking));
    }
    return Promise.reject(error);
  });
}
