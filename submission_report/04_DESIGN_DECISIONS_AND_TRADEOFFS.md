# 04 Design Decisions And Tradeoffs

## Why PostgreSQL

PostgreSQL is the system of record for:

- `queries`
- `search_logs`
- `system_config`

It supports transactional updates, relational aggregation, and persistence across restarts.

## Why Redis

Redis is used for prefix suggestion caching because it provides fast read access for hot prefixes and supports a simple key-value cache shape.

Current cache keys use the form:

- `prefix:<prefix>:<ranking>`

## Why 3 Redis Nodes

The backend routes cache entries across 3 standalone Redis nodes:

- `localhost:6379`
- `localhost:6380`
- `localhost:6381`

This keeps the deployment simple while spreading cache load across multiple nodes.

## Why Consistent Hashing

The current implementation uses a client-side hash ring:

- `String.hashCode()` produces the hash
- the hash is mapped into 16,384 slots
- each slot is assigned to one of 3 contiguous ranges
- the router chooses the Redis node for the prefix

This keeps routing deterministic and avoids server-side Redis Cluster complexity.

## Cache Strategy

Current behavior:

- Cache hits return directly from Redis
- Cache misses query PostgreSQL and then populate Redis
- Batch flushes invalidate affected prefix entries

This gives a blended model:

- lazy population for new prefixes
- startup warmup for hot prefixes
- invalidation on writes to keep cache aligned with PostgreSQL

## Warmup Strategy

Current warmup implementation:

- scans the top 50,000 queries ordered by `global_count DESC`
- generates prefixes of length 3 and 4 only
- warms both `trending` and `global`

Latest verified warmup numbers:

- scanned queries: 50,000
- unique prefixes: 14,405
- cache entries generated: 28,810

This is a bounded warmup, not a full-prefix precompute.

## Batch Aggregation

The batch layer consists of:

- `BatchBuffer`
- `BatchWriter`
- `BatchFlushScheduler`

Current behavior:

- `SearchService` writes each search to `search_logs`
- the buffer aggregates queries in memory
- flush occurs when the buffer reaches 10 unique queries or 30 seconds have elapsed
- `BatchWriter` updates `queries`
- `BatchWriter` marks matching `search_logs` rows as batched
- `BatchWriter` advances virtual time by 60 seconds per flush
- `BatchWriter` invalidates all affected prefixes in Redis

## Virtual Time

Virtual time exists so the search statistics can progress in a fixed 2006 context.

Current behavior:

- `VirtualTimeManager` loads the saved virtual time on startup
- it falls back to `config/virtual_time.json` if needed
- it falls back again to the configured reference date if required
- `getVirtualTime()` returns saved virtual time plus real elapsed time
- `VirtualTimeShutdownHook` persists the final virtual time on shutdown

## Tradeoffs

### Precompute vs fully lazy

- Precompute improves first-hit behavior for hot prefixes
- Fully lazy would reduce startup work but increase the first request latency for each prefix
- The current bounded warmup is a compromise

### Redis memory vs startup time

- Larger warmup sizes increase cache coverage
- Larger warmup sizes also increase startup time and cache footprint
- The current 50k warmup was measured at more than 23 minutes, so larger warmups were not justified

### String.hashCode routing vs stronger hashing

- `String.hashCode()` is simple and deterministic
- it can produce mild imbalance for natural-language prefixes
- a stronger hash or virtual-node scheme could improve balance

### Simplicity vs perfect balance

- The current routing model is easy to understand and test
- it is good enough for a course assignment
- it does not guarantee perfect distribution

## Redis Distribution Findings

### Original skew

Before cleanup, the live key distribution was:

- node 1: 11,536 keys
- node 2: 8,030 keys
- node 3: 8,886 keys

Average:

- 9,484 keys per node

Deviation:

- node 1: +21.6%
- node 2: -15.3%
- node 3: -6.3%

### Cause analysis

The audit showed:

- no routing mismatches
- no slot mapping bug
- no slot-range bug
- no uneven warmup behavior in the current code path

The skew came from a combination of:

- dataset prefix characteristics
- the `String.hashCode()` routing model
- stale 1- and 2-character keys from earlier warmup runs

### Cleanup

The obsolete 1- and 2-character keys were removed from Redis.

### Final snapshot after cleanup

Latest verified post-cleanup distribution:

- node 1: 9,844 keys
- node 2: 7,972 keys
- node 3: 8,812 keys

Average:

- 8,876 keys per node

Deviation:

- node 1: +10.9%
- node 2: -10.2%
- node 3: -0.7%

The current distribution is acceptable for the assignment, though it is not perfectly even.

