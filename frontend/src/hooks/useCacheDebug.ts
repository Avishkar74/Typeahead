import { useCallback, useState } from 'react';
import type { CacheDebugResponse } from '../types/api';
import { fetchCacheDebug } from '../services/cacheDebugService';

interface UseCacheDebugState {
  data: CacheDebugResponse | null;
  loading: boolean;
  error: string | null;
}

export function useCacheDebug() {
  const [state, setState] = useState<UseCacheDebugState>({ data: null, loading: false, error: null });

  const lookup = useCallback(async (prefix: string) => {
    if (!prefix.trim()) return;
    setState({ data: null, loading: true, error: null });
    try {
      const data = await fetchCacheDebug(prefix.trim());
      setState({ data, loading: false, error: null });
    } catch (err: unknown) {
      setState({ data: null, loading: false, error: err instanceof Error ? err.message : 'Unknown error' });
    }
  }, []);

  return { ...state, lookup };
}
