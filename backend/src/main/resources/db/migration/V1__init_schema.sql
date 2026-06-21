-- 1. Create queries table
CREATE TABLE queries (
  id BIGSERIAL PRIMARY KEY,
  query_text VARCHAR(255) NOT NULL UNIQUE,
  query_lower VARCHAR(255) NOT NULL,
  global_count INTEGER DEFAULT 0,
  weekly_count INTEGER DEFAULT 0,
  daily_count INTEGER DEFAULT 0,
  trending_score DECIMAL(10,2) DEFAULT 0,
  first_searched_at TIMESTAMP,
  last_searched_at TIMESTAMP,
  trending_score_calculated_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for queries table
CREATE INDEX idx_queries_prefix_trending 
ON queries(query_lower, trending_score DESC)
WHERE query_lower IS NOT NULL;

CREATE INDEX idx_queries_prefix_global 
ON queries(query_lower, global_count DESC)
WHERE query_lower IS NOT NULL;

CREATE INDEX idx_queries_trending_score 
ON queries(trending_score DESC);


-- 2. Create search_logs table
CREATE TABLE search_logs (
  id BIGSERIAL PRIMARY KEY,
  query_lower VARCHAR(255) NOT NULL,
  query_text VARCHAR(255),
  virtual_searched_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  batched BOOLEAN DEFAULT FALSE,
  batched_at TIMESTAMP
);

-- Indexes for search_logs table
CREATE INDEX idx_search_logs_batched 
ON search_logs(batched, created_at);

CREATE INDEX idx_search_logs_query 
ON search_logs(query_lower);

CREATE INDEX idx_search_logs_virtual_time 
ON search_logs(virtual_searched_at);


-- 3. Create system_config table
CREATE TABLE system_config (
  id SERIAL PRIMARY KEY,
  config_key VARCHAR(100) UNIQUE NOT NULL,
  config_value TEXT NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed last_virtual_time configuration
INSERT INTO system_config (config_key, config_value) 
VALUES ('last_virtual_time', '2006-05-31T23:59:56');
