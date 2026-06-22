# 02 Dataset And Loading

## Dataset Source

The project uses the AOL Search Query Log dataset from 2006.

Current dataset files in the repository:

- `dataset/search_logs.csv`
- `dataset/queries_count.csv`

`search_logs.csv` is the raw search-event dataset. `queries_count.csv` is the aggregated dataset generated from `search_logs.csv`.

The repository also contains `dataset/Typeahead_Dataset_Report.md`, which documents the original source statistics and preprocessing notes.

## Current Dataset Sizes

The running database currently contains:

- `queries`: 1,244,165 rows
- `search_logs`: 2,969,636 rows

These are the live database counts observed on the current stack.

## Dataset Loader

`DatasetLoader` is the startup importer in `backend/src/main/java/com/typeahed/backend/service/DatasetLoader.java`.

Current behavior:

- Runs on `ApplicationReadyEvent`
- Is ordered before other application-ready listeners
- Does nothing when `app.dataset-import.enabled=false`
- Checks `queries` first and skips query import if rows already exist
- Checks `search_logs` first and skips log import if rows already exist
- Reads CSV files in a streaming manner
- Uses JDBC batch updates for ingestion
- Writes `queries` rows from `queries_count.csv`
- Writes `search_logs` rows from `search_logs.csv`

The current default configuration disables dataset import:

- `app.dataset-import.enabled=false`

## Docker Persistence

The project uses named Docker volumes in `docker-compose.yml`:

- PostgreSQL volume: `postgres-data:/var/lib/postgresql/data`
- Redis Node 1 volume: `redis-node-1-data:/data`
- Redis Node 2 volume: `redis-node-2-data:/data`
- Redis Node 3 volume: `redis-node-3-data:/data`

Verified persistence result:

- PostgreSQL row counts stayed identical across `docker compose down` and `docker compose up -d`
- Redis key counts stayed identical across the same restart
- The running containers reattached to the same named volumes after restart

This means the dataset and cache survive container restarts without re-importing or re-warming.

