package com.smppulse.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class FileDataSource implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FileDataSource.class);

    private final Path filePath;
    private final List<String> lines;
    private final AtomicInteger lineIndex = new AtomicInteger(0);

    public FileDataSource(Path filePath) throws IOException {
        this.filePath = filePath;
        this.lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            throw new IOException("File is empty: " + filePath);
        }
        log.info("Loaded {} lines from {}", lines.size(), filePath);
    }

    public String getLine(String mode) {
        return switch (mode.toLowerCase()) {
            case "random" -> lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
            case "sequential" -> {
                int idx = lineIndex.getAndUpdate(i -> (i + 1) % lines.size());
                yield lines.get(idx);
            }
            default -> lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
        };
    }

    public String getLineByIndex(int index) {
        if (index < 0 || index >= lines.size()) {
            return lines.get(0);
        }
        return lines.get(index);
    }

    public int size() {
        return lines.size();
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public void close() {
        // Lines are in memory, nothing to close
    }
}
