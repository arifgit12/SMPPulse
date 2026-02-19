package com.smppulse.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterResolver {

    private static final Logger log = LoggerFactory.getLogger(ParameterResolver.class);

    private final RandomResolver randomResolver = new RandomResolver();
    private final SequenceResolver sequenceResolver = new SequenceResolver();
    private final TimestampResolver timestampResolver = new TimestampResolver();
    private final UuidResolver uuidResolver = new UuidResolver();
    private final RangeResolver rangeResolver = new RangeResolver();
    private final DataSourceManager dataSourceManager;
    private final TemplateParser templateParser = new TemplateParser();

    public ParameterResolver(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public String resolveTemplate(String template) {
        return templateParser.resolve(template, this);
    }

    public String resolve(PlaceholderToken token) {
        try {
            return switch (token.function()) {
                case "random.digits" -> randomResolver.resolveDigits(token.getIntArg(0, 10));
                case "random.alpha" -> randomResolver.resolveAlpha(token.getIntArg(0, 10));
                case "random.alphanumeric" -> randomResolver.resolveAlphanumeric(token.getIntArg(0, 10));
                case "sequence" -> sequenceResolver.resolve();
                case "uuid" -> uuidResolver.resolve();
                case "timestamp" -> {
                    if (token.hasArgs()) {
                        yield timestampResolver.resolve(token.getArg(0).trim());
                    }
                    yield timestampResolver.resolve();
                }
                case "range" -> {
                    long min = token.getLongArg(0, 0);
                    long max = token.getLongArg(1, 100);
                    yield rangeResolver.resolve(min, max);
                }
                default -> {
                    // Handle colon-based expressions: file:path:line, csv:path:column
                    if (token.function().startsWith("file:")) {
                        yield resolveFile(token);
                    } else if (token.function().startsWith("csv:")) {
                        yield resolveCsv(token);
                    } else if (token.function().contains(":")) {
                        // timestamp:format shorthand
                        String[] parts = token.function().split(":", 2);
                        if (parts[0].equals("timestamp")) {
                            yield timestampResolver.resolve(parts[1]);
                        }
                        yield token.fullExpression();
                    } else {
                        log.warn("Unknown placeholder: {}", token.fullExpression());
                        yield token.fullExpression();
                    }
                }
            };
        } catch (Exception e) {
            log.warn("Error resolving placeholder {}: {}", token.fullExpression(), e.getMessage());
            return token.fullExpression();
        }
    }

    private String resolveFile(PlaceholderToken token) {
        // ${file:path:line} where line mode is "random" or "sequential"
        String expr = token.function();
        String[] parts = expr.split(":", 3);
        if (parts.length < 2) return token.fullExpression();

        String path = parts[1];
        String mode = parts.length > 2 ? parts[2] : "random";

        try {
            FileDataSource source = dataSourceManager.getFileSource(path);
            return source.getLine(mode);
        } catch (Exception e) {
            log.warn("Failed to read file source {}: {}", path, e.getMessage());
            return token.fullExpression();
        }
    }

    private String resolveCsv(PlaceholderToken token) {
        // ${csv:path:column}
        String expr = token.function();
        String[] parts = expr.split(":", 3);
        if (parts.length < 3) return token.fullExpression();

        String path = parts[1];
        String column = parts[2];

        try {
            CsvDataSource source = dataSourceManager.getCsvSource(path);
            return source.getValue(column);
        } catch (Exception e) {
            log.warn("Failed to read CSV source {}: {}", path, e.getMessage());
            return token.fullExpression();
        }
    }

    public void resetSequence() {
        sequenceResolver.reset();
    }

    public TemplateParser getTemplateParser() {
        return templateParser;
    }
}
