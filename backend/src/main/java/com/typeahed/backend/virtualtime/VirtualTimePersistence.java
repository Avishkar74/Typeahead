package com.typeahed.backend.virtualtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typeahed.backend.config.AppProperties;
import com.typeahed.backend.entity.SystemConfig;
import com.typeahed.backend.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
public class VirtualTimePersistence {

    private static final Logger logger = LoggerFactory.getLogger(VirtualTimePersistence.class);
    private static final String CONFIG_KEY = "last_virtual_time";
    private static final String BACKUP_FILE_PATH = "config/virtual_time.json";
    private static final String DEFAULT_REFERENCE_DATE = "2006-05-31T23:59:56";

    private final SystemConfigRepository systemConfigRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public VirtualTimePersistence(SystemConfigRepository systemConfigRepository,
                                  AppProperties appProperties,
                                  ObjectMapper objectMapper,
                                  Clock clock) {
        this.systemConfigRepository = systemConfigRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public LocalDateTime load() {
        // 1. Try system_config table
        try {
            Optional<SystemConfig> config = systemConfigRepository.findByConfigKey(CONFIG_KEY);
            if (config.isPresent()) {
                String val = config.get().getConfigValue();
                if (val != null && !val.trim().isEmpty()) {
                    LocalDateTime vt = LocalDateTime.parse(val.trim());
                    logger.info("Loaded virtual time from database: {}", vt);
                    return vt;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load virtual time from database, trying backup file...", e);
        }

        // 2. Try backup file
        File backupFile = new File(BACKUP_FILE_PATH);
        if (backupFile.exists() && backupFile.isFile()) {
            try {
                Map<?, ?> data = objectMapper.readValue(backupFile, Map.class);
                if (data != null && data.containsKey("virtual_time")) {
                    String val = (String) data.get("virtual_time");
                    if (val != null && !val.trim().isEmpty()) {
                        LocalDateTime vt = LocalDateTime.parse(val.trim());
                        logger.info("Loaded virtual time from backup file: {}", vt);
                        return vt;
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to load virtual time from backup file, falling back to default...", e);
            }
        }

        // 3. Documented default virtual time
        String defaultDateStr = null;
        if (appProperties != null && appProperties.getVirtualTime() != null) {
            defaultDateStr = appProperties.getVirtualTime().getReferenceDate();
        }
        if (defaultDateStr == null || defaultDateStr.trim().isEmpty()) {
            defaultDateStr = DEFAULT_REFERENCE_DATE;
        }

        try {
            LocalDateTime vt = LocalDateTime.parse(defaultDateStr.trim());
            logger.info("Loaded default reference virtual time: {}", vt);
            return vt;
        } catch (Exception e) {
            logger.error("Failed to parse default reference date: {}. Using absolute fallback.", defaultDateStr, e);
            return LocalDateTime.parse(DEFAULT_REFERENCE_DATE);
        }
    }

    public void save(LocalDateTime virtualTime) {
        // 1. Save to PostgreSQL
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(CONFIG_KEY);
            SystemConfig config;
            if (configOpt.isPresent()) {
                config = configOpt.get();
            } else {
                config = new SystemConfig();
                config.setConfigKey(CONFIG_KEY);
            }
            config.setConfigValue(virtualTime.toString());
            systemConfigRepository.save(config);
            logger.info("Saved virtual time to database: {}", virtualTime);
        } catch (Exception e) {
            logger.error("Failed to save virtual time to database: {}", e.getMessage(), e);
        }

        // 2. Save to local file backup
        try {
            File backupFile = new File(BACKUP_FILE_PATH);
            File parentDir = backupFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }

            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("virtual_time", virtualTime.toString());
            rootNode.put("saved_at", LocalDateTime.now(clock).toString());
            rootNode.put("app_version", "1.0");

            objectMapper.writeValue(backupFile, rootNode);
            logger.info("Saved virtual time to backup file: {}", virtualTime);
        } catch (Exception e) {
            logger.warn("Failed to save local backup file: {}", e.getMessage(), e);
        }
    }
}
