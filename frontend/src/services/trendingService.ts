import type { TrendingResponse } from '../types/api';
import { ApiResponseError, get } from './apiClient';
import { fallbackTrending } from './offlineFallback';

export function fetchTrending(): Promise<TrendingResponse> {
  return get<TrendingResponse>('/trending').catch((error: unknown) => {
    if (error instanceof ApiResponseError && [502, 503, 504].includes(error.status)) {
      return Promise.resolve(fallbackTrending());
    }
    if (error instanceof TypeError) {
      return Promise.resolve(fallbackTrending());
    }
    return Promise.reject(error);
  });
}
