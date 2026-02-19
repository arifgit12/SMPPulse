package com.smppulse.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);

    private final Map<String, FileDataSource> fileSources = new ConcurrentHashMap<>();
    private final Map<String, CsvDataSource> csvSources = new ConcurrentHashMap<>();

    public FileDataSource getFileSource(String path) throws IOException {
        return fileSources.computeIfAbsent(path, p -> {
            try {
                return new FileDataSource(Path.of(p));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load file: " + p, e);
            }
        });
    }

    public CsvDataSource getCsvSource(String path) throws IOException {
        return csvSources.computeIfAbsent(path, p -> {
            try {
                return new CsvDataSource(Path.of(p));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load CSV: " + p, e);
            }
        });
    }

    public void closeAll() {
        fileSources.values().forEach(FileDataSource::close);
        fileSources.clear();
        csvSources.values().forEach(CsvDataSource::close);
        csvSources.clear();
        log.info("All data sources closed");
    }
}
