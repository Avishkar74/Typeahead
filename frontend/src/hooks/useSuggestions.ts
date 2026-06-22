import { useCallback, useEffect, useRef, useState } from 'react';
import type { RankingMode, SuggestionResponse } from '../types/api';
import { fetchSuggestions } from '../services/suggestionService';
import { useDebounce } from './useDebounce';

interface UseSuggestionsState {
  data: SuggestionResponse | null;
  loading: boolean;
  error: string | null;
}

export function useSuggestions(query: string, ranking: RankingMode, debounceMs = 300) {
  const [state, setState] = useState<UseSuggestionsState>({ data: null, loading: false, error: null });
  const debouncedQuery = useDebounce(query, debounceMs);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const trimmed = debouncedQuery.trim();
    abortRef.current?.abort();

    if (trimmed.length < 2) {
      return;
    }

    const controller = new AbortController();
    abortRef.current = controller;
    let active = true;

    const run = async () => {
      setState((prev) => ({ ...prev, loading: true, error: null }));
      try {
        const data = await fetchSuggestions(trimmed, ranking, controller.signal);
        if (!active || controller.signal.aborted) return;
        setState({ data, loading: false, error: null });
      } catch (err: unknown) {
        if (!active || controller.signal.aborted || (err instanceof Error && err.name === 'AbortError')) {
          return;
        }
        setState({
          data: null,
          loading: false,
          error: err instanceof Error ? err.message : 'Unknown error',
        });
      }
    };

    void run();

    return () => {
      active = false;
      abortRef.current?.abort();
    };
  }, [debouncedQuery, ranking]);

  const clear = useCallback(() => {
    setState({ data: null, loading: false, error: null });
  }, []);

  return { ...state, clear };
}
