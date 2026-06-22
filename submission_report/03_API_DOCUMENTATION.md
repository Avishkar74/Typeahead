# 03 API Documentation

## POST /search

Purpose:

- Record a search submission

Request:

```http
POST /search
Content-Type: application/json

{
  "query": "iphone"
}
```

Request fields:

- `query` string, required, non-blank, max 255 characters

Response:

```json
{
  "message": "Searched"
}
```

Notes:

- The query is saved to `search_logs`
- The query is also queued into the batch buffer

## GET /suggest

Purpose:

- Return up to 10 suggestions for a prefix

Request:

```http
GET /suggest?q=iph&ranking=trending
```

Query parameters:

- `q` required prefix string
- `ranking` optional, allowed values: `trending` or `global`
- Default `ranking` is `trending`

Minimum prefix length:

- The current frontend and backend treat prefixes shorter than 3 characters as non-searchable
- For `q` lengths 0, 1, or 2, the service returns an empty result set and does not query PostgreSQL or Redis

Response:

```json
{
  "prefix": "iph",
  "ranking_type": "trending",
  "results": [
    {
      "query": "iphone",
      "global_count": 10000,
      "weekly_count": 200,
      "daily_count": 50,
      "trending_score": 6062.00
    }
  ],
  "cache_hit": true
}
```

Response fields:

- `prefix`
- `ranking_type`
- `results`
- `cache_hit`

`results` items use the `SuggestionDto` shape:

- `query`
- `global_count`
- `weekly_count`
- `daily_count`
- `trending_score`

## GET /trending

Purpose:

- Return the top 10 trending queries overall

Request:

```http
GET /trending
```

Response:

```json
{
  "results": [
    {
      "query": "google",
      "global_count": 32396,
      "weekly_count": 0,
      "daily_count": 0,
      "trending_score": 19437.60
    }
  ]
}
```

Response fields:

- `results`

## GET /api/debug/cache

Purpose:

- Inspect Redis routing for a prefix

Request:

```http
GET /api/debug/cache?prefix=iph
```

Query parameters:

- `prefix` required

Behavior:

- Returns `cache_hit=false` for prefixes shorter than 3 characters
- Does not perform a Redis lookup for prefixes shorter than 3 characters

Response:

```json
{
  "prefix": "iph",
  "responsible_node": "localhost:6381",
  "slot": 12000,
  "cache_hit": true
}
```

Response fields:

- `prefix`
- `responsible_node`
- `slot`
- `cache_hit`

## GET /api/virtual-time

Purpose:

- Return the current virtual time snapshot

Request:

```http
GET /api/virtual-time
```

Response:

```json
{
  "virtual_time": "2006-06-01T00:05:04",
  "reference_time": "2006-05-31T23:59:56",
  "real_time_elapsed_seconds": 304,
  "batch_flush_increment_seconds": 60
}
```

Response fields:

- `virtual_time`
- `reference_time`
- `real_time_elapsed_seconds`
- `batch_flush_increment_seconds`

