package com.typeahed.backend.virtualtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.entity.SystemConfig;
import com.typeahed.backend.repository.SystemConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VirtualTimePersistenceTest {

    private static final String BACKUP_FILE_PATH = "config/virtual_time.json";

    private SystemConfigRepository repository;
    private AppProperties appProperties;
    private ObjectMapper objectMapper;
    private Clock clock;
    private VirtualTimePersistence persistence;

    private File backupFile;
    private File backupDir;
    private boolean originalFileExisted = false;
    private byte[] originalFileContent = null;

    @BeforeEach
    void setUp() throws IOException {
        repository = mock(SystemConfigRepository.class);
        appProperties = mock(AppProperties.class);
        objectMapper = new ObjectMapper();
        clock = Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneId.systemDefault());
        persistence = new VirtualTimePersistence(repository, appProperties, objectMapper, clock);

        backupFile = new File(BACKUP_FILE_PATH);
        backupDir = backupFile.getParentFile();

        // Backup existing file if any to avoid disturbing real environment
        if (backupFile.exists()) {
            originalFileExisted = true;
            originalFileContent = Files.readAllBytes(backupFile.toPath());
            Files.delete(backupFile.toPath());
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test file
        if (backupFile.exists()) {
            Files.delete(backupFile.toPath());
        }

        // Restore original file if any
        if (originalFileExisted && originalFileContent != null) {
            if (backupDir != null && !backupDir.exists()) {
                Files.createDirectories(backupDir.toPath());
            }
            Files.write(backupFile.toPath(), originalFileContent);
        }
    }

    @Test
    void testLoadFromDatabaseSuccess() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("last_virtual_time");
        config.setConfigValue("2006-06-01T12:00:00");

        when(repository.findByConfigKey("last_virtual_time")).thenReturn(Optional.of(config));

        LocalDateTime loaded = persistence.load();
        assertThat(loaded).isEqualTo(LocalDateTime.of(2006, 6, 1, 12, 0, 0));
        verify(repository, times(1)).findByConfigKey("last_virtual_time");
    }

    @Test
    void testLoadFromBackupFileWhenDatabaseFails() throws IOException {
        // Mock DB to throw exception
        when(repository.findByConfigKey(any())).thenThrow(new RuntimeException("DB offline"));

        // Seed local backup file
        if (backupDir != null && !backupDir.exists()) {
            Files.createDirectories(backupDir.toPath());
        }
        String json = "{\"virtual_time\":\"2006-06-02T15:30:45\",\"saved_at\":\"2026-06-22T00:00:00\",\"app_version\":\"1.0\"}";
        Files.writeString(backupFile.toPath(), json);

        LocalDateTime loaded = persistence.load();
        assertThat(loaded).isEqualTo(LocalDateTime.of(2006, 6, 2, 15, 30, 45));
    }

    @Test
    void testLoadFallbackToPropertiesReferenceDate() {
        when(repository.findByConfigKey(any())).thenReturn(Optional.empty());
        AppProperties.VirtualTime vtProp = new AppProperties.VirtualTime();
        vtProp.setReferenceDate("2006-06-03T10:15:30");
        when(appProperties.getVirtualTime()).thenReturn(vtProp);

        LocalDateTime loaded = persistence.load();
        assertThat(loaded).isEqualTo(LocalDateTime.of(2006, 6, 3, 10, 15, 30));
    }

    @Test
    void testLoadAbsoluteFallbackWhenAllFailures() {
        when(repository.findByConfigKey(any())).thenThrow(new RuntimeException("DB offline"));
        when(appProperties.getVirtualTime()).thenReturn(null);

        LocalDateTime loaded = persistence.load();
        assertThat(loaded).isEqualTo(LocalDateTime.of(2006, 5, 31, 23, 59, 56));
    }

    @Test
    void testSavePersistsToDatabaseAndFile() throws IOException {
        LocalDateTime timeToSave = LocalDateTime.of(2006, 6, 4, 18, 0, 0);

        // Mock DB load for save (it does a findByConfigKey first)
        SystemConfig existingConfig = new SystemConfig();
        existingConfig.setConfigKey("last_virtual_time");
        existingConfig.setConfigValue("2006-05-31T23:59:56");
        when(repository.findByConfigKey("last_virtual_time")).thenReturn(Optional.of(existingConfig));

        persistence.save(timeToSave);

        // Verify database interaction
        ArgumentCaptor<SystemConfig> configCaptor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(repository, times(1)).save(configCaptor.capture());
        assertThat(configCaptor.getValue().getConfigValue()).isEqualTo(timeToSave.toString());

        // Verify file interaction
        assertThat(backupFile.exists()).isTrue();
        Map<?, ?> fileData = objectMapper.readValue(backupFile, Map.class);
        assertThat(fileData.get("virtual_time")).isEqualTo(timeToSave.toString());
        assertThat(fileData.get("app_version")).isEqualTo("1.0");
        assertThat(fileData.get("saved_at")).isEqualTo(LocalDateTime.now(clock).toString());
    }

    @Test
    void testSaveGracefulDegradationWhenDatabaseFails() throws IOException {
        LocalDateTime timeToSave = LocalDateTime.of(2006, 6, 4, 18, 0, 0);
        doThrow(new RuntimeException("DB save failed")).when(repository).save(any());

        // Ensure saving still writes to file backup even if DB write throws exception
        persistence.save(timeToSave);

        assertThat(backupFile.exists()).isTrue();
        Map<?, ?> fileData = objectMapper.readValue(backupFile, Map.class);
        assertThat(fileData.get("virtual_time")).isEqualTo(timeToSave.toString());
    }
}
