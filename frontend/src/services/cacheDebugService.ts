import type { CacheDebugResponse } from '../types/api';
import { ApiResponseError, get } from './apiClient';
import { fallbackCacheDebug } from './offlineFallback';

export function fetchCacheDebug(prefix: string): Promise<CacheDebugResponse> {
  return get<CacheDebugResponse>('/api/debug/cache', { prefix }).catch((error: unknown) => {
    if (error instanceof ApiResponseError && [502, 503, 504].includes(error.status)) {
      return Promise.resolve(fallbackCacheDebug(prefix));
    }
    if (error instanceof TypeError) {
      return Promise.resolve(fallbackCacheDebug(prefix));
    }
    return Promise.reject(error);
  });
}
