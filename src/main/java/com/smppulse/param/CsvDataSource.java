package com.smppulse.param;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvDataSource implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CsvDataSource.class);

    private final Path filePath;
    private final List<String[]> rows;
    private final String[] headers;
    private final AtomicInteger rowIndex = new AtomicInteger(0);

    public CsvDataSource(Path filePath) throws IOException {
        this.filePath = filePath;
        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                throw new IOException("CSV file is empty: " + filePath);
            }
            this.headers = allRows.getFirst();
            this.rows = allRows.subList(1, allRows.size());
            if (rows.isEmpty()) {
                throw new IOException("CSV file has no data rows: " + filePath);
            }
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV file: " + filePath, e);
        }
        log.info("Loaded {} rows from CSV {}", rows.size(), filePath);
    }

    public String getValue(String column) {
        int colIndex = findColumnIndex(column);
        if (colIndex < 0) return "";

        int idx = rowIndex.getAndUpdate(i -> (i + 1) % rows.size());
        String[] row = rows.get(idx);
        return colIndex < row.length ? row[colIndex] : "";
    }

    public String getRandomValue(String column) {
        int colIndex = findColumnIndex(column);
        if (colIndex < 0) return "";

        String[] row = rows.get(ThreadLocalRandom.current().nextInt(rows.size()));
        return colIndex < row.length ? row[colIndex] : "";
    }

    private int findColumnIndex(String column) {
        // Try as number first
        try {
            int idx = Integer.parseInt(column.trim());
            if (idx >= 0 && idx < headers.length) return idx;
        } catch (NumberFormatException ignored) {}

        // Try as header name
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(column.trim())) {
                return i;
            }
        }

        log.warn("Column '{}' not found in CSV {}", column, filePath);
        return -1;
    }

    public String[] getHeaders() { return headers; }
    public int rowCount() { return rows.size(); }
    public Path getFilePath() { return filePath; }

    @Override
    public void close() {
        // Data is in memory
    }
}
