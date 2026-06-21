package com.typeahed.backend.service;

import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.repository.QueryRepository;
import com.typeahed.backend.repository.SearchLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class DatasetLoaderTest {

    private AppProperties appProperties;
    private JdbcTemplate jdbcTemplate;
    private QueryRepository queryRepository;
    private SearchLogRepository searchLogRepository;
    private DatasetLoader datasetLoader;

    @TempDir
    Path tempDir;

    private File queriesFile;
    private File logsFile;

    @BeforeEach
    void setUp() throws IOException {
        appProperties = new AppProperties();
        jdbcTemplate = mock(JdbcTemplate.class);
        queryRepository = mock(QueryRepository.class);
        searchLogRepository = mock(SearchLogRepository.class);

        datasetLoader = new DatasetLoader(appProperties, jdbcTemplate, queryRepository, searchLogRepository);

        // Create sample CSV files
        queriesFile = tempDir.resolve("queries_count.csv").toFile();
        try (FileWriter writer = new FileWriter(queriesFile)) {
            writer.write("Query,Global Count,Weekly Count,Daily Count,Trending Score\n");
            writer.write("google,32396,2141,219,20101.8\n");
            writer.write("yahoo,13344,963,102,8305.5\n");
            writer.write("map quest,2695,205,22,1680.7\n");
            writer.write("invalid_line_no_commas\n");
            writer.write(",1,2,3,4.0\n"); // empty query, should be skipped
        }

        logsFile = tempDir.resolve("search_logs.csv").toFile();
        try (FileWriter writer = new FileWriter(logsFile)) {
            writer.write("Query,QueryTime\n");
            writer.write("family guy,2006-03-01 16:01:20\n");
            writer.write("also sprach zarathustra,2006-03-02 14:48:55\n");
            writer.write("invalid_line_no_date\n");
            writer.write(",2006-03-02 14:48:55\n"); // empty query, should be skipped
        }

        appProperties.getDatasetImport().setQueriesFilePath(queriesFile.getAbsolutePath());
        appProperties.getDatasetImport().setLogsFilePath(logsFile.getAbsolutePath());
        appProperties.getDatasetImport().setBatchSize(2);

        // Default mock behavior for batchUpdate: return array of 1s of correct batch size
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenAnswer(invocation -> {
                    BatchPreparedStatementSetter setter = invocation.getArgument(1);
                    int[] res = new int[setter.getBatchSize()];
                    Arrays.fill(res, 1);
                    return res;
                });
    }

    @Test
    void testImportDisabled() {
        appProperties.getDatasetImport().setEnabled(false);

        datasetLoader.onApplicationReady();

        verifyNoInteractions(queryRepository);
        verifyNoInteractions(searchLogRepository);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testImportSkippedWhenTablesNotEmpty() {
        appProperties.getDatasetImport().setEnabled(true);
        when(queryRepository.count()).thenReturn(5L);
        when(searchLogRepository.count()).thenReturn(10L);

        datasetLoader.onApplicationReady();

        verify(queryRepository).count();
        verify(searchLogRepository).count();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testImportQueriesOnlyWhenLogsNotEmpty() throws Exception {
        appProperties.getDatasetImport().setEnabled(true);
        when(queryRepository.count()).thenReturn(0L);
        when(searchLogRepository.count()).thenReturn(10L);

        datasetLoader.onApplicationReady();

        verify(queryRepository).count();
        verify(searchLogRepository).count();

        // Should call batchUpdate for queries, but not logs.
        // With 3 valid rows and batchSize = 2, we expect 2 calls: one with batch of size 2, one with batch of size 1.
        verify(jdbcTemplate, times(2)).batchUpdate(contains("INSERT INTO queries"), any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, never()).batchUpdate(contains("INSERT INTO search_logs"), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void testImportLogsOnlyWhenQueriesNotEmpty() throws Exception {
        appProperties.getDatasetImport().setEnabled(true);
        when(queryRepository.count()).thenReturn(5L);
        when(searchLogRepository.count()).thenReturn(0L);

        datasetLoader.onApplicationReady();

        verify(queryRepository).count();
        verify(searchLogRepository).count();

        // Should call batchUpdate for logs, but not queries.
        // With 2 valid rows and batchSize = 2, we expect exactly 1 call.
        verify(jdbcTemplate, never()).batchUpdate(contains("INSERT INTO queries"), any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, times(1)).batchUpdate(contains("INSERT INTO search_logs"), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void testImportQueriesDataBinding() throws SQLException {
        appProperties.getDatasetImport().setEnabled(true);
        appProperties.getDatasetImport().setBatchSize(10); // set batch size large enough to hold all rows in 1 batch
        when(queryRepository.count()).thenReturn(0L);
        when(searchLogRepository.count()).thenReturn(10L); // skip logs

        datasetLoader.onApplicationReady();

        ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO queries"), setterCaptor.capture());

        BatchPreparedStatementSetter setter = setterCaptor.getValue();
        assertThat(setter.getBatchSize()).isEqualTo(3); // 3 valid lines (google, yahoo, map quest)

        PreparedStatement ps = mock(PreparedStatement.class);

        // Verify first row: google,32396,2141,219,20101.8
        setter.setValues(ps, 0);
        verify(ps).setString(1, "google");
        verify(ps).setString(2, "google");
        verify(ps).setInt(3, 32396);
        verify(ps).setInt(4, 2141);
        verify(ps).setInt(5, 219);
        verify(ps).setBigDecimal(6, new BigDecimal("20101.8"));

        // Verify third row: map quest,2695,205,22,1680.7
        reset(ps);
        setter.setValues(ps, 2);
        verify(ps).setString(1, "map quest");
        verify(ps).setString(2, "map quest");
        verify(ps).setInt(3, 2695);
        verify(ps).setInt(4, 205);
        verify(ps).setInt(5, 22);
        verify(ps).setBigDecimal(6, new BigDecimal("1680.7"));
    }

    @Test
    void testImportLogsDataBinding() throws SQLException {
        appProperties.getDatasetImport().setEnabled(true);
        appProperties.getDatasetImport().setBatchSize(10);
        when(queryRepository.count()).thenReturn(5L); // skip queries
        when(searchLogRepository.count()).thenReturn(0L);

        datasetLoader.onApplicationReady();

        ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO search_logs"), setterCaptor.capture());

        BatchPreparedStatementSetter setter = setterCaptor.getValue();
        assertThat(setter.getBatchSize()).isEqualTo(2); // 2 valid lines

        PreparedStatement ps = mock(PreparedStatement.class);

        // Verify first row: family guy,2006-03-01 16:01:20
        setter.setValues(ps, 0);
        verify(ps).setString(1, "family guy");
        verify(ps).setString(2, "family guy");
        verify(ps).setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2006, 3, 1, 16, 1, 20)));

        // Verify second row: also sprach zarathustra,2006-03-02 14:48:55
        reset(ps);
        setter.setValues(ps, 1);
        verify(ps).setString(1, "also sprach zarathustra");
        verify(ps).setString(2, "also sprach zarathustra");
        verify(ps).setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2006, 3, 2, 14, 48, 55)));
    }
}
