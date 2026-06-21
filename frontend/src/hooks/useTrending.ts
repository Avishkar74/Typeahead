import { useCallback, useState } from 'react';
import type { TrendingResponse } from '../types/api';
import { fetchTrending } from '../services/trendingService';

interface UseTrendingState {
  data: TrendingResponse | null;
  loading: boolean;
  error: string | null;
}

export function useTrending() {
  const [state, setState] = useState<UseTrendingState>({ data: null, loading: false, error: null });

  const load = useCallback(async () => {
    setState({ data: null, loading: true, error: null });
    try {
      const data = await fetchTrending();
      setState({ data, loading: false, error: null });
    } catch (err: unknown) {
      setState({ data: null, loading: false, error: err instanceof Error ? err.message : 'Unknown error' });
    }
  }, []);

  return { ...state, load };
}
