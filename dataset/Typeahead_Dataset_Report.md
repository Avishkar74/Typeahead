# Typeahead System – Dataset & Schema Report

## Dataset Source

The dataset used for this project is derived from the **AOL Search Query Log Dataset (2006)**.

Data processing was performed in two stages:

1. **search_logs.csv** – Raw/cleaned search logs obtained from the AOL dataset after preprocessing.
2. **queries_count.csv** – Aggregated dataset generated from `search_logs.csv` and used by the Typeahead system.

---

## Data Pipeline

```text
AOL Search Query Log
        │
        ▼
search_logs.csv
(Raw Search Events)
        │
        ▼
Aggregation Pipeline
        │
        ▼
queries_count.csv
(Query Statistics)
```

### search_logs.csv

This dataset contains individual search events before aggregation.

| Column | Type | Description |
|----------|----------|----------|
| Query | String | User search query |
| QueryTime | Timestamp | Time when the query was searched |

### Google Drive Link

**search_logs.csv:**

> https://drive.google.com/file/d/1XIbyjLBMxoSptTvZXeK65uIVr-RJ2LWZ/view?usp=sharing

---

## queries_count.csv

This dataset is obtained by aggregating the records present in `search_logs.csv`.

After preprocessing:

- Removed invalid queries
- Removed queries with length < 2
- Aggregated duplicate queries
- Computed popularity and recency metrics

### Final Schema

| Column | Type | Description |
|----------|----------|----------|
| Query | String | Search query text |
| Global Count | Integer | Total occurrences of the query across the dataset |
| Weekly Count | Integer | Number of occurrences within the previous 7-day rolling window |
| Daily Count | Integer | Number of occurrences within the previous 24-hour rolling window |
| Trending Score | Float | Weighted popularity score derived from Global, Weekly, and Daily counts |

### Notes

The Weekly Count and Daily Count are calculated using the query timestamp as the reference point:

- Daily Window = QueryTime − 24 hours → QueryTime
- Weekly Window = QueryTime − 7 days → QueryTime

These are rolling windows and not calendar-based days or weeks.

### Google Drive Link

**queries_count.csv:**

> https://drive.google.com/file/d/1931-OYamJ8ggTzkpPG1nt1R_SDbh1HDg/view?usp=sharing

---

## Dataset Statistics

### search_logs.csv

| Metric | Value |
|----------|----------|
| Records Before Filtering | 3,053,204 |
| Records After Filtering | 2,969,752 |

### queries_count.csv

| Metric | Value |
|----------|----------|
| Unique Queries | 1,244,453 |

---

## Purpose

The aggregated dataset is used to:

- Generate Typeahead suggestions
- Rank suggestions by popularity
- Support trending query detection
- Support Redis cache precomputation
- Enable fast prefix-based lookups in the Typeahead service
