package com.typeahed.backend.service;

import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.repository.QueryRepository;
import com.typeahed.backend.repository.SearchLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatasetLoader {

    private static final Logger logger = LoggerFactory.getLogger(DatasetLoader.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppProperties appProperties;
    private final JdbcTemplate jdbcTemplate;
    private final QueryRepository queryRepository;
    private final SearchLogRepository searchLogRepository;

    public DatasetLoader(AppProperties appProperties,
                         JdbcTemplate jdbcTemplate,
                         QueryRepository queryRepository,
                         SearchLogRepository searchLogRepository) {
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.queryRepository = queryRepository;
        this.searchLogRepository = searchLogRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!appProperties.getDatasetImport().isEnabled()) {
            logger.info("Dataset import is disabled by configuration (app.dataset-import.enabled=false). Skipping import.");
            return;
        }

        logger.info("Dataset import enabled. Initiating database verification check...");
        long startTime = System.currentTimeMillis();

        try {
            importQueriesIfEmpty();
        } catch (Exception e) {
            logger.error("Queries import failed: {}", e.getMessage(), e);
        }

        try {
            importLogsIfEmpty();
        } catch (Exception e) {
            logger.error("Search logs import failed: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Dataset loader process finished. Total duration: {}ms", duration);
    }

    public void importQueriesIfEmpty() throws Exception {
        if (queryRepository.count() > 0) {
            logger.info("Queries table already contains records. Skipping queries import.");
            return;
        }

        String filePath = appProperties.getDatasetImport().getQueriesFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("Queries CSV file not found at path: {}. Skipping queries import.", filePath);
            return;
        }

        logger.info("Starting streaming queries import from: {} ...", filePath);
        long startTime = System.currentTimeMillis();

        long processed = 0;
        long imported = 0;
        long skipped = 0;

        int batchSize = appProperties.getDatasetImport().getBatchSize();
        List<QueryRow> batch = new ArrayList<>(batchSize);

        String sql = "INSERT INTO queries (query_text, query_lower, global_count, weekly_count, daily_count, trending_score, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW()) ON CONFLICT (query_text) DO NOTHING";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // read header
            String line;
            while ((line = br.readLine()) != null) {
                processed++;
                QueryRow row = parseQueriesCountLine(line);
                if (row == null) {
                    skipped++;
                    continue;
                }

                batch.add(row);
                if (batch.size() >= batchSize) {
                    int inserted = executeQueriesBatch(sql, batch);
                    imported += inserted;
                    skipped += (batch.size() - inserted);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                int inserted = executeQueriesBatch(sql, batch);
                imported += inserted;
                skipped += (batch.size() - inserted);
                batch.clear();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Queries import complete: processed={}, imported={}, skipped={}, duration={}ms",
                processed, imported, skipped, duration);
    }

    public void importLogsIfEmpty() throws Exception {
        if (searchLogRepository.count() > 0) {
            logger.info("Search logs table already contains records. Skipping search logs import.");
            return;
        }

        String filePath = appProperties.getDatasetImport().getLogsFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("Search logs CSV file not found at path: {}. Skipping logs import.", filePath);
            return;
        }

        logger.info("Starting streaming search logs import from: {} ...", filePath);
        long startTime = System.currentTimeMillis();

        long processed = 0;
        long imported = 0;
        long skipped = 0;

        int batchSize = appProperties.getDatasetImport().getBatchSize();
        List<LogRow> batch = new ArrayList<>(batchSize);

        String sql = "INSERT INTO search_logs (query_text, query_lower, virtual_searched_at, created_at, batched) " +
                "VALUES (?, ?, ?, NOW(), true)";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // read header
            String line;
            while ((line = br.readLine()) != null) {
                processed++;
                LogRow row = parseSearchLogsLine(line);
                if (row == null) {
                    skipped++;
                    continue;
                }

                batch.add(row);
                if (batch.size() >= batchSize) {
                    int inserted = executeLogsBatch(sql, batch);
                    imported += inserted;
                    skipped += (batch.size() - inserted);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                int inserted = executeLogsBatch(sql, batch);
                imported += inserted;
                skipped += (batch.size() - inserted);
                batch.clear();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Search logs import complete: processed={}, imported={}, skipped={}, duration={}ms",
                processed, imported, skipped, duration);
    }

    private int executeQueriesBatch(String sql, List<QueryRow> batch) {
        List<QueryRow> batchCopy = new ArrayList<>(batch);
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                QueryRow row = batchCopy.get(i);
                ps.setString(1, row.queryText);
                ps.setString(2, row.queryLower);
                ps.setInt(3, row.globalCount);
                ps.setInt(4, row.weeklyCount);
                ps.setInt(5, row.dailyCount);
                ps.setBigDecimal(6, row.trendingScore);
            }

            @Override
            public int getBatchSize() {
                return batchCopy.size();
            }
        });

        int inserted = 0;
        for (int r : results) {
            if (r > 0) {
                inserted++;
            }
        }
        return inserted;
    }

    private int executeLogsBatch(String sql, List<LogRow> batch) {
        List<LogRow> batchCopy = new ArrayList<>(batch);
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LogRow row = batchCopy.get(i);
                ps.setString(1, row.queryText);
                ps.setString(2, row.queryLower);
                ps.setTimestamp(3, Timestamp.valueOf(row.virtualSearchedAt));
            }

            @Override
            public int getBatchSize() {
                return batchCopy.size();
            }
        });

        int inserted = 0;
        for (int r : results) {
            if (r > 0) {
                inserted++;
            }
        }
        return inserted;
    }

    private QueryRow parseQueriesCountLine(String line) {
        try {
            int len = line.length();
            int idx = len;
            int nextIdx = len;
            String[] parts = new String[5];

            for (int i = 4; i >= 1; i--) {
                idx = line.lastIndexOf(',', idx - 1);
                if (idx == -1) {
                    return null;
                }
                parts[i] = line.substring(idx + 1, nextIdx).trim();
                nextIdx = idx;
            }

            parts[0] = line.substring(0, idx).trim();
            if (parts[0].isEmpty() || parts[0].length() > 255) {
                return null;
            }

            String queryText = parts[0];
            String queryLower = queryText.toLowerCase().trim();
            int globalCount = Integer.parseInt(parts[1]);
            int weeklyCount = Integer.parseInt(parts[2]);
            int dailyCount = Integer.parseInt(parts[3]);
            BigDecimal trendingScore = new BigDecimal(parts[4]);

            return new QueryRow(queryText, queryLower, globalCount, weeklyCount, dailyCount, trendingScore);
        } catch (Exception e) {
            logger.warn("Skipping malformed queries count line: '{}'. Error: {}", line, e.getMessage());
            return null; // Malformed row
        }
    }

    private LogRow parseSearchLogsLine(String line) {
        try {
            int idx = line.lastIndexOf(',');
            if (idx == -1) {
                return null;
            }

            String queryText = line.substring(0, idx).trim();
            if (queryText.isEmpty() || queryText.length() > 255) {
                return null;
            }

            String queryLower = queryText.toLowerCase().trim();
            String timeString = line.substring(idx + 1).trim();
            LocalDateTime virtualSearchedAt = LocalDateTime.parse(timeString, DATE_FORMATTER);

            return new LogRow(queryText, queryLower, virtualSearchedAt);
        } catch (Exception e) {
            logger.warn("Skipping malformed search logs line: '{}'. Error: {}", line, e.getMessage());
            return null; // Malformed row
        }
    }

    private static class QueryRow {
        final String queryText;
        final String queryLower;
        final int globalCount;
        final int weeklyCount;
        final int dailyCount;
        final java.math.BigDecimal trendingScore;

        QueryRow(String queryText, String queryLower, int globalCount, int weeklyCount, int dailyCount, BigDecimal trendingScore) {
            this.queryText = queryText;
            this.queryLower = queryLower;
            this.globalCount = globalCount;
            this.weeklyCount = weeklyCount;
            this.dailyCount = dailyCount;
            this.trendingScore = trendingScore;
        }
    }

    private static class LogRow {
        final String queryText;
        final String queryLower;
        final LocalDateTime virtualSearchedAt;

        LogRow(String queryText, String queryLower, LocalDateTime virtualSearchedAt) {
            this.queryText = queryText;
            this.queryLower = queryLower;
            this.virtualSearchedAt = virtualSearchedAt;
        }
    }
}
