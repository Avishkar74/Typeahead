// ─── Suggestion ──────────────────────────────────────────────────────────────

export interface SuggestionDto {
  query: string;
  global_count: number;
  weekly_count: number;
  daily_count: number;
  trending_score: number;
}

export type RankingMode = 'trending' | 'global';

export interface SuggestionResponse {
  prefix: string;
  ranking_type: string;
  results: SuggestionDto[];
  cache_hit: boolean;
}

// ─── Search ───────────────────────────────────────────────────────────────────

export interface SearchRequest {
  query: string;
}

export interface SearchResponse {
  message: string;
}

// ─── Trending ─────────────────────────────────────────────────────────────────

export interface TrendingResponse {
  results: SuggestionDto[];
}

// ─── Cache Debug ──────────────────────────────────────────────────────────────

export interface CacheDebugResponse {
  prefix: string;
  responsible_node: string;
  slot: number;
  cache_hit: boolean;
}

// ─── API Error ────────────────────────────────────────────────────────────────

export interface ApiError {
  status: number;
  message: string;
}
