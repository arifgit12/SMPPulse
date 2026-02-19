package com.smppulse.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ResultExporter {

    private static final Logger log = LoggerFactory.getLogger(ResultExporter.class);
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final MetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;

    public ResultExporter(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---- CSV ---------------------------------------------------------------

    public void exportCsv(Path filePath) throws IOException {
        MetricsSnapshot snapshot = metricsCollector.snapshot();
        double[] tpsHistory = metricsCollector.getTpsTracker().getRecentTps(300);

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Summary section
            writer.writeNext(new String[]{"=== SMPPulse Load Test Report ==="});
            writer.writeNext(new String[]{"Metric", "Value"});
            writer.writeNext(new String[]{"Submitted", String.valueOf(snapshot.submitted())});
            writer.writeNext(new String[]{"Success", String.valueOf(snapshot.success())});
            writer.writeNext(new String[]{"Failed", String.valueOf(snapshot.failed())});
            writer.writeNext(new String[]{"Throttled", String.valueOf(snapshot.throttled())});
            writer.writeNext(new String[]{"Timeouts", String.valueOf(snapshot.timeouts())});
            writer.writeNext(new String[]{"Success Rate %", String.format("%.2f", snapshot.successRate())});
            writer.writeNext(new String[]{});
            writer.writeNext(new String[]{"=== Latency (ms) ==="});
            writer.writeNext(new String[]{"P50", String.valueOf(snapshot.latencyP50())});
            writer.writeNext(new String[]{"P95", String.valueOf(snapshot.latencyP95())});
            writer.writeNext(new String[]{"P99", String.valueOf(snapshot.latencyP99())});
            writer.writeNext(new String[]{"Avg", String.format("%.2f", snapshot.latencyAvg())});
            writer.writeNext(new String[]{"Min", String.valueOf(snapshot.latencyMin())});
            writer.writeNext(new String[]{"Max", String.valueOf(snapshot.latencyMax())});
            writer.writeNext(new String[]{});
            writer.writeNext(new String[]{"=== Throughput ==="});
            writer.writeNext(new String[]{"Current TPS", String.format("%.2f", snapshot.currentTps())});
            writer.writeNext(new String[]{"Average TPS", String.format("%.2f", snapshot.averageTps())});
            writer.writeNext(new String[]{"Peak TPS", String.format("%.2f", snapshot.peakTps())});
            writer.writeNext(new String[]{});
            writer.writeNext(new String[]{"=== DLR ==="});
            writer.writeNext(new String[]{"Received", String.valueOf(snapshot.dlrReceived())});
            writer.writeNext(new String[]{"Pending", String.valueOf(snapshot.dlrPending())});
            writer.writeNext(new String[]{"Timed Out", String.valueOf(snapshot.dlrTimedOut())});
            writer.writeNext(new String[]{"DLR Success", String.valueOf(snapshot.dlrSuccess())});
            writer.writeNext(new String[]{"DLR Failed", String.valueOf(snapshot.dlrFailed())});
            writer.writeNext(new String[]{});
            writer.writeNext(new String[]{"=== Timing ==="});
            writer.writeNext(new String[]{"Duration", snapshot.elapsed().toString()});
            if (snapshot.startTime() != null) {
                writer.writeNext(new String[]{"Start Time", DISPLAY_FMT.format(snapshot.startTime())});
            }
            if (snapshot.endTime() != null) {
                writer.writeNext(new String[]{"End Time", DISPLAY_FMT.format(snapshot.endTime())});
            }

            // TPS time-series
            writer.writeNext(new String[]{});
            writer.writeNext(new String[]{"=== TPS Time Series ==="});
            writer.writeNext(new String[]{"Second", "TPS"});
            for (int i = 0; i < tpsHistory.length; i++) {
                if (tpsHistory[i] > 0) {
                    writer.writeNext(new String[]{String.valueOf(i), String.format("%.0f", tpsHistory[i])});
                }
            }
        }
        log.info("Results exported to CSV: {}", filePath);
    }

    // ---- JSON --------------------------------------------------------------

    public void exportJson(Path filePath) throws IOException {
        MetricsSnapshot snapshot = metricsCollector.snapshot();
        double[] tpsHistory = metricsCollector.getTpsTracker().getRecentTps(300);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("report", "SMPPulse Load Test Report");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("submitted", snapshot.submitted());
        summary.put("success", snapshot.success());
        summary.put("failed", snapshot.failed());
        summary.put("throttled", snapshot.throttled());
        summary.put("timeouts", snapshot.timeouts());
        summary.put("successRate", snapshot.successRate());
        data.put("summary", summary);

        Map<String, Object> latency = new LinkedHashMap<>();
        latency.put("p50", snapshot.latencyP50());
        latency.put("p95", snapshot.latencyP95());
        latency.put("p99", snapshot.latencyP99());
        latency.put("avg", snapshot.latencyAvg());
        latency.put("min", snapshot.latencyMin());
        latency.put("max", snapshot.latencyMax());
        data.put("latency", latency);

        Map<String, Object> tps = new LinkedHashMap<>();
        tps.put("current", snapshot.currentTps());
        tps.put("average", snapshot.averageTps());
        tps.put("peak", snapshot.peakTps());
        data.put("tps", tps);

        Map<String, Object> dlr = new LinkedHashMap<>();
        dlr.put("received", snapshot.dlrReceived());
        dlr.put("pending", snapshot.dlrPending());
        dlr.put("timedOut", snapshot.dlrTimedOut());
        dlr.put("success", snapshot.dlrSuccess());
        dlr.put("failed", snapshot.dlrFailed());
        data.put("dlr", dlr);

        data.put("duration", snapshot.elapsed().toString());
        data.put("startTime", snapshot.startTime());
        data.put("endTime", snapshot.endTime());

        // TPS time-series
        List<Map<String, Object>> tpsSeries = new ArrayList<>();
        for (int i = 0; i < tpsHistory.length; i++) {
            if (tpsHistory[i] > 0) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("second", i);
                point.put("tps", tpsHistory[i]);
                tpsSeries.add(point);
            }
        }
        data.put("tpsTimeSeries", tpsSeries);

        objectMapper.writeValue(filePath.toFile(), data);
        log.info("Results exported to JSON: {}", filePath);
    }

    // ---- HTML --------------------------------------------------------------

    public void exportHtml(Path filePath) throws IOException {
        MetricsSnapshot snapshot = metricsCollector.snapshot();
        double[] tpsHistory = metricsCollector.getTpsTracker().getRecentTps(300);

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(filePath))) {
            w.println("<!DOCTYPE html>");
            w.println("<html lang=\"en\"><head><meta charset=\"UTF-8\">");
            w.println("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
            w.println("<title>SMPPulse Report</title>");
            writeHtmlStyles(w);
            w.println("</head><body>");

            // Header
            w.println("<div class=\"header\">");
            w.println("  <h1>SMPPulse Load Test Report</h1>");
            w.printf("  <p class=\"subtitle\">Generated %s</p>%n",
                    DISPLAY_FMT.format(java.time.Instant.now()));
            if (snapshot.startTime() != null) {
                w.printf("  <p class=\"subtitle\">Test: %s &mdash; %s &nbsp; (Duration: %s)</p>%n",
                        DISPLAY_FMT.format(snapshot.startTime()),
                        snapshot.endTime() != null ? DISPLAY_FMT.format(snapshot.endTime()) : "running",
                        formatDuration(snapshot.elapsed()));
            }
            w.println("</div>");

            // Counter cards
            w.println("<div class=\"cards\">");
            writeCard(w, "Submitted", fmt(snapshot.submitted()), "");
            writeCard(w, "Success", fmt(snapshot.success()), "green");
            writeCard(w, "Failed", fmt(snapshot.failed()), "red");
            writeCard(w, "Throttled", fmt(snapshot.throttled()), "orange");
            writeCard(w, "Timeouts", fmt(snapshot.timeouts()), "red");
            writeCard(w, "Success Rate", String.format("%.1f%%", snapshot.successRate()),
                    snapshot.successRate() >= 99 ? "green" : snapshot.successRate() >= 90 ? "orange" : "red");
            w.println("</div>");

            // TPS cards
            w.println("<h2>Throughput</h2>");
            w.println("<div class=\"cards\">");
            writeCard(w, "Current TPS", String.format("%.0f", snapshot.currentTps()), "purple");
            writeCard(w, "Average TPS", String.format("%.1f", snapshot.averageTps()), "purple");
            writeCard(w, "Peak TPS", String.format("%.0f", snapshot.peakTps()), "purple");
            w.println("</div>");

            // TPS Chart (inline SVG)
            w.println("<h2>TPS Over Time</h2>");
            writeTpsChart(w, tpsHistory);

            // Latency
            w.println("<h2>Latency</h2>");
            w.println("<div class=\"cards\">");
            writeCard(w, "P50", snapshot.latencyP50() + " ms", "");
            writeCard(w, "P95", snapshot.latencyP95() + " ms", "orange");
            writeCard(w, "P99", snapshot.latencyP99() + " ms", "red");
            writeCard(w, "Avg", String.format("%.1f ms", snapshot.latencyAvg()), "");
            writeCard(w, "Min", snapshot.latencyMin() + " ms", "");
            writeCard(w, "Max", snapshot.latencyMax() + " ms", "");
            w.println("</div>");

            // Latency bar chart
            writeLatencyBarChart(w, snapshot);

            // DLR
            w.println("<h2>Delivery Reports</h2>");
            w.println("<div class=\"cards\">");
            writeCard(w, "Received", fmt(snapshot.dlrReceived()), "");
            writeCard(w, "Pending", fmt(snapshot.dlrPending()), "orange");
            writeCard(w, "Timed Out", fmt(snapshot.dlrTimedOut()), "red");
            writeCard(w, "DLR Success", fmt(snapshot.dlrSuccess()), "green");
            writeCard(w, "DLR Failed", fmt(snapshot.dlrFailed()), "red");
            w.println("</div>");

            // Result distribution pie (CSS pie)
            w.println("<h2>Result Distribution</h2>");
            writeResultPie(w, snapshot);

            // Raw data table
            w.println("<h2>Full Metrics</h2>");
            writeMetricsTable(w, snapshot);

            // Footer
            w.println("<div class=\"footer\">SMPPulse v1.0.0 &mdash; SMPP Load Testing</div>");
            w.println("</body></html>");
        }
        log.info("Results exported to HTML: {}", filePath);
    }

    // ---- HTML Helpers ------------------------------------------------------

    private void writeHtmlStyles(PrintWriter w) {
        w.println("<style>");
        w.println("""
            * { margin:0; padding:0; box-sizing:border-box; }
            body { font-family:'Segoe UI',system-ui,sans-serif; background:#1e1e2e; color:#e0e0e0; padding:24px; }
            .header { text-align:center; margin-bottom:32px; }
            .header h1 { font-size:28px; color:#a78bfa; margin-bottom:4px; }
            .subtitle { color:#888898; font-size:13px; }
            h2 { color:#a78bfa; font-size:18px; margin:28px 0 12px; border-bottom:1px solid #3a3a5c; padding-bottom:6px; }
            .cards { display:flex; flex-wrap:wrap; gap:12px; }
            .card { background:#2a2a3d; border:1px solid #3a3a5c; border-radius:10px; padding:16px 20px;
                    min-width:140px; flex:1; text-align:center; }
            .card .value { font-size:26px; font-weight:700; }
            .card .label { font-size:11px; color:#888898; margin-top:4px; text-transform:uppercase; letter-spacing:.5px; }
            .card .value.green  { color:#059669; }
            .card .value.red    { color:#dc2626; }
            .card .value.orange { color:#d97706; }
            .card .value.purple { color:#7c3aed; }
            table { width:100%; border-collapse:collapse; background:#2a2a3d; border-radius:8px; overflow:hidden; margin-top:8px; }
            th { background:#16162a; color:#a78bfa; text-align:left; padding:10px 14px; font-size:12px;
                 text-transform:uppercase; letter-spacing:.5px; }
            td { padding:8px 14px; border-top:1px solid #3a3a5c; font-size:13px; }
            tr:hover td { background:#32324a; }
            .chart-box { background:#16162a; border:1px solid #3a3a5c; border-radius:10px; padding:16px; margin-top:8px; overflow-x:auto; }
            svg text { fill:#888898; font-size:11px; font-family:'Segoe UI',system-ui,sans-serif; }
            .bar-row { display:flex; align-items:center; gap:10px; margin:6px 0; }
            .bar-label { min-width:36px; font-size:13px; font-weight:600; }
            .bar-track { flex:1; height:22px; background:#2a2a3d; border-radius:4px; overflow:hidden; }
            .bar-fill  { height:100%; border-radius:4px; transition:width .3s; }
            .bar-value { min-width:70px; text-align:right; font-family:monospace; font-size:13px; }
            .pie-container { display:flex; align-items:center; gap:32px; margin-top:8px; }
            .pie-legend span { display:inline-block; width:12px; height:12px; border-radius:3px; margin-right:6px; vertical-align:middle; }
            .pie-legend p { margin:6px 0; font-size:13px; }
            .footer { text-align:center; color:#555; font-size:11px; margin-top:40px; padding-top:16px; border-top:1px solid #3a3a5c; }
            @media print { body { background:#fff; color:#222; } .card { border-color:#ccc; } }
            """);
        w.println("</style>");
    }

    private void writeCard(PrintWriter w, String label, String value, String color) {
        String cls = color.isEmpty() ? "value" : "value " + color;
        w.printf("<div class=\"card\"><div class=\"%s\">%s</div><div class=\"label\">%s</div></div>%n",
                cls, value, label);
    }

    private void writeTpsChart(PrintWriter w, double[] tpsHistory) {
        // Find non-zero range
        int start = 0, end = tpsHistory.length - 1;
        while (start < tpsHistory.length && tpsHistory[start] == 0) start++;
        while (end > start && tpsHistory[end] == 0) end--;
        if (start > end) {
            w.println("<div class=\"chart-box\"><p style=\"color:#888898;text-align:center;\">No TPS data recorded</p></div>");
            return;
        }

        int count = end - start + 1;
        double maxTps = 1;
        for (int i = start; i <= end; i++) maxTps = Math.max(maxTps, tpsHistory[i]);

        int svgW = Math.max(count * 6, 600);
        int svgH = 200;
        int padL = 50, padR = 16, padT = 10, padB = 30;
        int plotW = svgW - padL - padR;
        int plotH = svgH - padT - padB;

        w.printf("<div class=\"chart-box\"><svg width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">%n",
                svgW, svgH, svgW, svgH);

        // Grid lines & Y labels
        for (int i = 0; i <= 4; i++) {
            int y = padT + plotH - (plotH * i / 4);
            double val = maxTps * i / 4;
            w.printf("  <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#2a2a3d\" stroke-width=\"1\"/>%n",
                    padL, y, padL + plotW, y);
            w.printf("  <text x=\"%d\" y=\"%d\" text-anchor=\"end\">%.0f</text>%n", padL - 6, y + 4, val);
        }

        // Build polyline points
        StringBuilder points = new StringBuilder();
        StringBuilder areaPoints = new StringBuilder();
        areaPoints.append(String.format("%d,%d ", padL, padT + plotH)); // bottom-left

        for (int i = 0; i < count; i++) {
            double v = tpsHistory[start + i];
            int x = padL + (int) ((double) i / Math.max(count - 1, 1) * plotW);
            int y = padT + plotH - (int) (v / maxTps * plotH);
            points.append(String.format("%d,%d ", x, y));
            areaPoints.append(String.format("%d,%d ", x, y));
        }
        areaPoints.append(String.format("%d,%d", padL + plotW, padT + plotH)); // bottom-right

        // Area fill
        w.printf("  <polygon points=\"%s\" fill=\"rgba(124,58,237,0.15)\" stroke=\"none\"/>%n", areaPoints);
        // Line
        w.printf("  <polyline points=\"%s\" fill=\"none\" stroke=\"#7c3aed\" stroke-width=\"2\"/>%n", points);

        // X axis labels
        int labelStep = Math.max(count / 6, 1);
        for (int i = 0; i < count; i += labelStep) {
            int x = padL + (int) ((double) i / Math.max(count - 1, 1) * plotW);
            w.printf("  <text x=\"%d\" y=\"%d\" text-anchor=\"middle\">%ds</text>%n",
                    x, svgH - 4, start + i);
        }

        w.println("</svg></div>");
    }

    private void writeLatencyBarChart(PrintWriter w, MetricsSnapshot s) {
        long max = Math.max(s.latencyP99(), 1);
        w.println("<div class=\"chart-box\">");
        writeBar(w, "P50", s.latencyP50(), max, "#059669");
        writeBar(w, "P95", s.latencyP95(), max, "#d97706");
        writeBar(w, "P99", s.latencyP99(), max, "#dc2626");
        writeBar(w, "Avg", (long) s.latencyAvg(), max, "#7c3aed");
        w.println("</div>");
    }

    private void writeBar(PrintWriter w, String label, long value, long max, String color) {
        double pct = (double) value / max * 100;
        w.printf("<div class=\"bar-row\">" +
                 "<div class=\"bar-label\">%s</div>" +
                 "<div class=\"bar-track\"><div class=\"bar-fill\" style=\"width:%.1f%%;background:%s;\"></div></div>" +
                 "<div class=\"bar-value\">%d ms</div></div>%n",
                label, pct, color, value);
    }

    private void writeResultPie(PrintWriter w, MetricsSnapshot s) {
        long total = s.success() + s.failed();
        if (total == 0) {
            w.println("<p style=\"color:#888898;\">No results yet</p>");
            return;
        }
        double successPct = (double) s.success() / total * 100;
        double failedPct = 100 - successPct;
        // CSS conic-gradient pie
        w.println("<div class=\"pie-container\">");
        w.printf("<div style=\"width:160px;height:160px;border-radius:50%%;background:conic-gradient(" +
                 "#059669 0%% %.1f%%,#dc2626 %.1f%% 100%%);flex-shrink:0;\"></div>%n",
                successPct, successPct);
        w.println("<div class=\"pie-legend\">");
        w.printf("<p><span style=\"background:#059669;\"></span>Success: %s (%.1f%%)</p>%n", fmt(s.success()), successPct);
        w.printf("<p><span style=\"background:#dc2626;\"></span>Failed: %s (%.1f%%)</p>%n", fmt(s.failed()), failedPct);
        w.println("</div></div>");
    }

    private void writeMetricsTable(PrintWriter w, MetricsSnapshot s) {
        w.println("<table><thead><tr><th>Metric</th><th>Value</th></tr></thead><tbody>");
        tableRow(w, "Total Submitted", fmt(s.submitted()));
        tableRow(w, "Success", fmt(s.success()));
        tableRow(w, "Failed", fmt(s.failed()));
        tableRow(w, "Throttled", fmt(s.throttled()));
        tableRow(w, "Timeouts", fmt(s.timeouts()));
        tableRow(w, "Success Rate", String.format("%.2f%%", s.successRate()));
        tableRow(w, "Latency P50", s.latencyP50() + " ms");
        tableRow(w, "Latency P95", s.latencyP95() + " ms");
        tableRow(w, "Latency P99", s.latencyP99() + " ms");
        tableRow(w, "Latency Avg", String.format("%.2f ms", s.latencyAvg()));
        tableRow(w, "Latency Min", s.latencyMin() + " ms");
        tableRow(w, "Latency Max", s.latencyMax() + " ms");
        tableRow(w, "Current TPS", String.format("%.1f", s.currentTps()));
        tableRow(w, "Average TPS", String.format("%.1f", s.averageTps()));
        tableRow(w, "Peak TPS", String.format("%.1f", s.peakTps()));
        tableRow(w, "DLR Received", fmt(s.dlrReceived()));
        tableRow(w, "DLR Pending", fmt(s.dlrPending()));
        tableRow(w, "DLR Timed Out", fmt(s.dlrTimedOut()));
        tableRow(w, "DLR Success", fmt(s.dlrSuccess()));
        tableRow(w, "DLR Failed", fmt(s.dlrFailed()));
        tableRow(w, "Duration", formatDuration(s.elapsed()));
        if (s.startTime() != null) tableRow(w, "Start Time", DISPLAY_FMT.format(s.startTime()));
        if (s.endTime() != null)   tableRow(w, "End Time", DISPLAY_FMT.format(s.endTime()));
        w.println("</tbody></table>");
    }

    private void tableRow(PrintWriter w, String metric, String value) {
        w.printf("<tr><td>%s</td><td>%s</td></tr>%n", metric, value);
    }

    // ---- Formatting helpers ------------------------------------------------

    private String fmt(long value) {
        if (value >= 1_000_000) return String.format("%,d (%.1fM)", value, value / 1_000_000.0);
        if (value >= 1_000) return String.format("%,d (%.1fK)", value, value / 1_000.0);
        return String.format("%,d", value);
    }

    private String formatDuration(java.time.Duration d) {
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }
}
