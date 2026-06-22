# 05 Performance Report

## Dataset Scale

Current live database counts:

- total queries: 1,244,165
- total logs: 2,969,636

## Cache Statistics

Latest observed live Redis key counts:

- Redis node 1: 8,230 keys
- Redis node 2: 6,752 keys
- Redis node 3: 7,574 keys
- total: 22,556 keys

Latest observed live Redis memory:

- Redis node 1: 10.86M
- Redis node 2: 8.89M
- Redis node 3: 10.08M

## Cache Warmup Results

Latest verified warmup run:

- warmup size: 50,000 queries
- prefixes generated: 14,405
- cache entries written: 28,810

Warmup prefix coverage:

- only 3- and 4-character prefixes are generated
- 1- and 2-character prefixes are excluded

## Startup Characteristics

Measured startup observations:

- Spring Boot application startup on the measurement instance: 7.821 seconds
- cache warmup began after application readiness
- cache warmup was still running after 23 minutes and 19 seconds in the measurement window

This means the current 50k warmup has a substantial startup cost.

## Redis Distribution Table

| Node | Keys | Memory | Deviation from average |
| --- | ---: | ---: | ---: |
| redis-node-1 | 8,230 | 10.86M | +9.5% |
| redis-node-2 | 6,752 | 8.89M | -10.2% |
| redis-node-3 | 7,574 | 10.08M | +0.7% |

## Performance Observations

- Cache hits are fast because the backend reads Redis first for searchable prefixes.
- Cache misses fall back to PostgreSQL and then populate Redis.
- Warmup improves first-request behavior for hot prefixes, but the current 50k warmup already takes a long time.
- The current design keeps Redis memory use reasonable while preserving a clear cache contract.
- The main cost in the current system is startup warmup time, not Redis memory usage.

## Limitations

- Redis counts can drift while the application is live, so cache snapshots are point-in-time measurements.
- The hash function is deterministic but not perfectly balanced for all prefix distributions.
- The current warmup size is intentionally bounded, so it does not precompute every possible prefix.
- The system keeps a persistent cache, but cache contents still depend on runtime writes and invalidation.

