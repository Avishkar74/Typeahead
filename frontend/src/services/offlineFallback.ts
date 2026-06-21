import type {
  CacheDebugResponse,
  SearchResponse,
  SuggestionDto,
  SuggestionResponse,
  TrendingResponse,
} from '../types/api';

const SAMPLE_SUGGESTIONS: SuggestionDto[] = [
  { query: 'what is my ip', global_count: 4120, weekly_count: 410, daily_count: 88, trending_score: 2640.2 },
  { query: 'what time is it', global_count: 3892, weekly_count: 388, daily_count: 91, trending_score: 2475.7 },
  { query: 'whatsapp web', global_count: 3011, weekly_count: 292, daily_count: 64, trending_score: 1934.4 },
  { query: 'weather today', global_count: 2874, weekly_count: 276, daily_count: 70, trending_score: 1810.9 },
  { query: 'where is my refund', global_count: 2211, weekly_count: 250, daily_count: 50, trending_score: 1544.3 },
  { query: 'walmart grocery', global_count: 2042, weekly_count: 201, daily_count: 44, trending_score: 1382.6 },
  { query: 'wikipedia', global_count: 1944, weekly_count: 182, daily_count: 31, trending_score: 1248.1 },
  { query: 'window cleaner', global_count: 1017, weekly_count: 93, daily_count: 18, trending_score: 614.2 },
  { query: 'wall street journal', global_count: 944, weekly_count: 87, daily_count: 16, trending_score: 566.8 },
  { query: 'watch series', global_count: 863, weekly_count: 79, daily_count: 12, trending_score: 511.4 },
];

const SAMPLE_TRENDING: SuggestionDto[] = [
  { query: 'google', global_count: 32396, weekly_count: 2141, daily_count: 219, trending_score: 20101.8 },
  { query: 'yahoo', global_count: 13344, weekly_count: 963, daily_count: 102, trending_score: 8305.5 },
  { query: 'ebay', global_count: 12949, weekly_count: 869, daily_count: 79, trending_score: 8038.0 },
  { query: 'mapquest', global_count: 8680, weekly_count: 561, daily_count: 66, trending_score: 5382.9 },
  { query: 'myspace', global_count: 7653, weekly_count: 626, daily_count: 51, trending_score: 4784.7 },
  { query: 'internet', global_count: 3854, weekly_count: 234, daily_count: 10, trending_score: 2383.6 },
  { query: 'facebook', global_count: 3750, weekly_count: 220, daily_count: 14, trending_score: 2270.5 },
  { query: 'youtube', global_count: 3621, weekly_count: 201, daily_count: 13, trending_score: 2144.1 },
  { query: 'walmart', global_count: 2811, weekly_count: 180, daily_count: 28, trending_score: 1761.8 },
  { query: 'weather', global_count: 2699, weekly_count: 177, daily_count: 34, trending_score: 1695.4 },
];

const CACHE_NODES = ['localhost:6379', 'localhost:6380', 'localhost:6381'] as const;

function hashString(input: string): number {
  let hash = 5381;
  for (let i = 0; i < input.length; i += 1) {
    hash = ((hash << 5) + hash) ^ input.charCodeAt(i);
  }
  return Math.abs(hash);
}

function pickNode(slot: number): string {
  const bucket = Math.floor((slot / 16384) * CACHE_NODES.length);
  return CACHE_NODES[Math.min(bucket, CACHE_NODES.length - 1)];
}

export function fallbackSuggestions(query: string, ranking: string): SuggestionResponse {
  const prefix = query.trim().toLowerCase();
  const results = SAMPLE_SUGGESTIONS.filter((item) => item.query.startsWith(prefix)).sort((a, b) => {
    const left = ranking === 'global' ? b.global_count - a.global_count : Number(b.trending_score) - Number(a.trending_score);
    return left;
  });

  return {
    prefix,
    ranking_type: ranking,
    results,
    cache_hit: false,
  };
}

export function fallbackTrending(): TrendingResponse {
  return { results: SAMPLE_TRENDING };
}

export function fallbackSearchResponse(query: string): SearchResponse {
  return {
    message: `Recorded "${query}" in offline demo mode`,
  };
}

export function fallbackCacheDebug(prefix: string): CacheDebugResponse {
  const normalized = prefix.trim().toLowerCase();
  const slot = hashString(normalized) % 16384;
  const responsibleNode = pickNode(slot);

  return {
    prefix: normalized,
    slot,
    responsible_node: responsibleNode,
    cache_hit: hashString(`${normalized}:cache`) % 2 === 0,
  };
}

