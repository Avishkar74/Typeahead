# Walkthrough

## Cache Behavior

The suggestion cache now starts **pre-warmed during application startup**.

Startup flow:

1. `DatasetLoader` finishes its `ApplicationReadyEvent` work first.
2. `CacheWarmupService` scans the top 50,000 queries ordered by `global_count DESC`.
3. It generates only 2-4 character prefixes from each query.
4. Duplicate prefixes are deduplicated.
5. Each prefix is warmed for both `trending` and `global`.
6. The first request for popular prefixes is usually a cache hit.

This matches the documented prefix cache strategy:

- cache key format: `prefix:<prefix>:<ranking>`
- TTL: 1 hour
- invalidation happens during batch flushes

### Prefix Generation Strategy

Only bounded prefixes are cached:

- 2 characters: `hi`
- 3 characters: `his`
- 4 characters: `hist`

For `history`, the warmup process stores:

- `hi`
- `his`
- `hist`

It does **not** cache:

- 1-character prefixes
- prefixes longer than 4 characters
- every possible substring

### Why 2-4 Characters?

This keeps the warmup memory footprint bounded while still covering the most common early typing patterns.

- 1-character prefixes are too broad and expensive.
- 2-4 characters catch the majority of first keystrokes where autocomplete value is highest.
- Longer prefixes are still filled lazily at runtime if needed.

### Memory Tradeoff

Warmup is intentionally selective:

- It avoids precomputing every prefix for every query.
- It warms only the most popular 50,000 queries.
- It stores each prefix once and shares the result across both ranking modes.

That gives a strong startup hit rate without letting Redis grow uncontrollably.

### Runtime Behavior

If a prefix is not already warm, the runtime path still works the same way:

1. `GET /suggest?q=<prefix>&ranking=<mode>` checks Redis.
2. If the key is missing, PostgreSQL is queried.
3. The response is stored in Redis.
4. A repeated request returns `cache_hit: true`.

## Virtual Time

Virtual time is exposed through `GET /api/virtual-time`.

The response includes:

- `virtual_time`
- `reference_time`
- `real_time_elapsed_seconds`
- `batch_flush_increment_seconds`

The backend uses the existing `VirtualTimeManager` as the source of truth.
