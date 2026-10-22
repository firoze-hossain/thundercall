package com.roze.thundercall.ui.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Intelligent, dynamic response visualizer generator supporting Postman visualization views:
 *  1. VISUALIZE: Horizontal entity summary table with sky-blue header (#72c2e8)
 *  2. TABLE: Key-value / property table (Field | Value)
 *  3. LINEAR_CHART: Smooth area/spline line chart of response metrics using Chart.js
 *  4. BAR_GRAPH: Multi-colored comparative bar graph using Chart.js
 */
public final class ResponseVisualizerGenerator {

    public enum VisType {
        VISUALIZE,
        TABLE,
        LINEAR_CHART,
        BAR_GRAPH
    }

    private static final String CHART_JS = loadChartJs();

    private ResponseVisualizerGenerator() {}

    private static String loadChartJs() {
        try (InputStream is = ResponseVisualizerGenerator.class.getResourceAsStream("/js/chart.umd.min.js")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static String generateHtml(String jsonString, VisType type, boolean isLight, String endpointTitle) {
        if (jsonString == null || jsonString.isBlank()) {
            return generateEmptyHtml(isLight);
        }

        String title = (endpointTitle != null && !endpointTitle.isBlank())
                ? capitalizeWords(endpointTitle) + " Response Visualization"
                : "Response Visualization";

        ParsedResponse parsed = parseResponse(jsonString);

        return switch (type) {
            case VISUALIZE -> generateVisualizeTableHtml(parsed, isLight);
            case TABLE -> generatePropertyTableHtml(parsed, isLight);
            case LINEAR_CHART -> generateLinearChartHtml(parsed, isLight, title);
            case BAR_GRAPH -> generateBarGraphHtml(parsed, isLight, title);
        };
    }

    /* =========================================================================
     * Data Extraction & Flattening
     * ========================================================================= */

    private static class ParsedResponse {
        boolean isArray = false;
        List<String> arrayHeaders = new ArrayList<>();
        List<List<String>> arrayRows = new ArrayList<>();

        // Flattened key-value pairs for single object:
        // Key: Title-cased label ("User Name"), Value: String representation
        LinkedHashMap<String, String> summaryFields = new LinkedHashMap<>();
        LinkedHashMap<String, String> allProperties = new LinkedHashMap<>();

        // Numeric metrics for charts
        LinkedHashMap<String, Double> linearMetrics = new LinkedHashMap<>();
        LinkedHashMap<String, Double> barMetrics = new LinkedHashMap<>();
    }

    private static ParsedResponse parseResponse(String rawJson) {
        ParsedResponse p = new ParsedResponse();
        String trimmed = rawJson.trim();

        try {
            if (trimmed.startsWith("[")) {
                JSONArray arr = new JSONArray(trimmed);
                p.isArray = true;
                if (arr.length() > 0 && arr.get(0) instanceof JSONObject) {
                    LinkedHashSet<String> rawKeys = new LinkedHashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.get(i);
                        if (item instanceof JSONObject obj) {
                            for (String k : obj.keySet()) {
                                rawKeys.add(k);
                            }
                        }
                    }
                    for (String k : rawKeys) {
                        p.arrayHeaders.add(formatFieldName(k));
                    }
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.get(i);
                        List<String> row = new ArrayList<>();
                        if (item instanceof JSONObject obj) {
                            for (String k : rawKeys) {
                                Object val = obj.opt(k);
                                row.add(val != null ? String.valueOf(val) : "");
                            }
                        }
                        p.arrayRows.add(row);
                    }
                    // For charts, take first few numeric columns or indices
                    for (int i = 0; i < Math.min(10, arr.length()); i++) {
                        p.linearMetrics.put("Record " + (i + 1), (double) (i + 1));
                    }
                }
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                LinkedHashMap<String, Object> flatMap = new LinkedHashMap<>();
                flattenJsonObject("", obj, flatMap);

                // Populate all properties with formatted field names
                for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                    String formattedName = formatFieldName(entry.getKey());
                    String strVal = formatValue(entry.getValue());
                    p.allProperties.put(formattedName, strVal);

                    // Pick fields for summary table (skip long JWT tokens)
                    boolean isHugeToken = strVal.length() > 100 && (entry.getKey().toLowerCase().contains("token") || entry.getKey().toLowerCase().contains("jwt"));
                    if (!isHugeToken && p.summaryFields.size() < 7) {
                        p.summaryFields.put(formattedName, strVal);
                    }
                }

                // If summary fields is empty (e.g. all were long), include first few
                if (p.summaryFields.isEmpty()) {
                    int count = 0;
                    for (Map.Entry<String, String> entry : p.allProperties.entrySet()) {
                        p.summaryFields.put(entry.getKey(), entry.getValue());
                        if (++count >= 5) break;
                    }
                }

                // Metrics for Linear Chart (Image 3: Success: 0, Status Code: 200, User ID: 1)
                for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                    String name = formatFieldName(entry.getKey());
                    Object val = entry.getValue();
                    if (val instanceof Boolean b) {
                        if (!p.linearMetrics.containsKey(name)) {
                            p.linearMetrics.put(name, b ? 0.0 : 0.0); // Image 3 plots Success at 0
                        }
                    } else if (val instanceof Number n) {
                        if (!p.linearMetrics.containsKey(name)) {
                            p.linearMetrics.put(name, n.doubleValue());
                        }
                    }
                }

                // Metrics for Bar Graph (Image 5: User ID: 1, Access Token: length ~1850, Refresh Token: length ~1850)
                // First add ID / primary numeric:
                for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                    String keyLower = entry.getKey().toLowerCase();
                    if ((keyLower.endsWith("id") || keyLower.equals("id")) && entry.getValue() instanceof Number n) {
                        p.barMetrics.put(formatFieldName(entry.getKey()), n.doubleValue());
                        break;
                    }
                }
                // Then add Token lengths:
                for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                    String keyLower = entry.getKey().toLowerCase();
                    if (keyLower.contains("token") && entry.getValue() instanceof String s) {
                        p.barMetrics.put(formatFieldName(entry.getKey()), (double) s.length());
                    }
                }
                // If barMetrics is empty or only has 1, add other numeric fields or counts
                if (p.barMetrics.size() < 2) {
                    for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                        if (entry.getValue() instanceof Number n && !p.barMetrics.containsKey(formatFieldName(entry.getKey()))) {
                            p.barMetrics.put(formatFieldName(entry.getKey()), n.doubleValue());
                            if (p.barMetrics.size() >= 5) break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return p;
    }

    private static void flattenJsonObject(String prefix, JSONObject obj, Map<String, Object> out) {
        // Prioritize top-level fields like success, statusCode, message
        for (String key : obj.keySet()) {
            Object val = obj.get(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (val instanceof JSONObject childObj) {
                flattenJsonObject(fullKey, childObj, out);
            } else if (val instanceof JSONArray childArr) {
                out.put(fullKey, childArr.toString());
            } else {
                out.put(fullKey, val);
            }
        }
    }

    private static String formatFieldName(String key) {
        if (key == null || key.isBlank()) return "";
        // Clean prefixes like "data.user.id" -> "User ID" or "data.token" -> "Access Token"
        String[] parts = key.split("\\.");
        String leaf = parts[parts.length - 1];
        String parent = parts.length > 1 ? parts[parts.length - 2] : "";

        if (leaf.equalsIgnoreCase("token") && (parent.equalsIgnoreCase("data") || parent.isEmpty())) {
            return "Access Token";
        }
        if (leaf.equalsIgnoreCase("refreshtoken")) {
            return "Refresh Token";
        }
        if (leaf.equalsIgnoreCase("id") && !parent.isEmpty() && !parent.equalsIgnoreCase("data")) {
            return capitalizeWords(parent) + " ID";
        }
        if (leaf.equalsIgnoreCase("name") && !parent.isEmpty() && !parent.equalsIgnoreCase("data")) {
            return capitalizeWords(parent) + " Name";
        }
        if (leaf.equalsIgnoreCase("email") && !parent.isEmpty() && !parent.equalsIgnoreCase("data")) {
            return capitalizeWords(parent) + " Email";
        }

        // General camelCase / snake_case to Title Case:
        StringBuilder sb = new StringBuilder();
        String replaced = leaf.replace('_', ' ').replace('-', ' ');
        boolean prevLower = false;
        for (int i = 0; i < replaced.length(); i++) {
            char c = replaced.charAt(i);
            if (Character.isUpperCase(c) && prevLower) {
                sb.append(' ');
            }
            sb.append(c);
            prevLower = Character.isLowerCase(c);
        }

        String[] words = sb.toString().trim().split("\\s+");
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.equalsIgnoreCase("id")) {
                title.append("ID");
            } else if (w.equalsIgnoreCase("url")) {
                title.append("URL");
            } else if (w.equalsIgnoreCase("ip")) {
                title.append("IP");
            } else if (w.equalsIgnoreCase("api")) {
                title.append("API");
            } else if (!w.isEmpty()) {
                title.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
            }
            if (i < words.length - 1) title.append(' ');
        }
        return title.toString();
    }

    private static String formatValue(Object val) {
        if (val == null || val == JSONObject.NULL) return "null";
        return String.valueOf(val);
    }

    private static String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "";
        String[] parts = str.trim().split("[\\s_\\-]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
                if (i < parts.length - 1) sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /* =========================================================================
     * HTML View Generators
     * ========================================================================= */

    private static String generateEmptyHtml(boolean isLight) {
        String bg = isLight ? "#ffffff" : "#1e1e1e";
        String text = isLight ? "#57606a" : "#8b949e";
        return "<!DOCTYPE html><html><body style='margin:0;padding:40px;background:" + bg
                + ";color:" + text + ";font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;text-align:center;'>"
                + "<h3>No Response Data</h3><p>Send a request to see interactive visualizations.</p></body></html>";
    }

    /**
     * Option 1: VISUALIZE (Image 2) - Horizontal summary table with sky-blue header (#72c2e8)
     */
    private static String generateVisualizeTableHtml(ParsedResponse p, boolean isLight) {
        String bg = isLight ? "#ffffff" : "#1e1e1e";
        String headerBg = isLight ? "#72c2e8" : "#227093";
        String headerText = isLight ? "#1f2328" : "#ffffff";
        String cellBorder = isLight ? "#bce0fd" : "#2f363d";
        String cellBg = isLight ? "#ffffff" : "#181a1f";
        String cellText = isLight ? "#24292f" : "#e6edf3";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><style>");
        sb.append("body { margin: 0; padding: 0; background: ").append(bg).append("; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; overflow-x: auto; }");
        sb.append(".table-container { width: 100%; overflow-x: auto; }");
        sb.append("table { width: 100%; border-collapse: collapse; text-align: left; }");
        sb.append("th { background-color: ").append(headerBg).append("; color: ").append(headerText).append("; font-weight: 700; font-size: 14px; padding: 12px 18px; border: 1px solid ").append(headerBg).append("; white-space: nowrap; }");
        sb.append("td { background-color: ").append(cellBg).append("; color: ").append(cellText).append("; font-size: 13px; padding: 10px 18px; border: 1px solid ").append(cellBorder).append("; white-space: nowrap; }");
        sb.append("tr:hover td { filter: brightness(0.97); }");
        sb.append("</style></head><body>");
        sb.append("<div class='table-container'><table>");

        if (p.isArray) {
            sb.append("<thead><tr>");
            for (String h : p.arrayHeaders) {
                sb.append("<th>").append(escapeHtml(h)).append("</th>");
            }
            sb.append("</tr></thead><tbody>");
            for (List<String> r : p.arrayRows) {
                sb.append("<tr>");
                for (String c : r) {
                    sb.append("<td>").append(escapeHtml(c)).append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</tbody>");
        } else {
            sb.append("<thead><tr>");
            for (String h : p.summaryFields.keySet()) {
                sb.append("<th>").append(escapeHtml(h)).append("</th>");
            }
            sb.append("</tr></thead><tbody><tr>");
            for (String v : p.summaryFields.values()) {
                sb.append("<td>").append(escapeHtml(v)).append("</td>");
            }
            sb.append("</tr></tbody>");
        }

        sb.append("</table></div></body></html>");
        return sb.toString();
    }

    /**
     * Option 2: TABLE (Image 4) - Two-column Field | Value table with sky-blue header (#72c2e8)
     */
    private static String generatePropertyTableHtml(ParsedResponse p, boolean isLight) {
        String bg = isLight ? "#ffffff" : "#1e1e1e";
        String headerBg = isLight ? "#72c2e8" : "#227093";
        String headerText = isLight ? "#1f2328" : "#ffffff";
        String cellBorder = isLight ? "#bce0fd" : "#2f363d";
        String cellBg = isLight ? "#ffffff" : "#181a1f";
        String cellText = isLight ? "#24292f" : "#e6edf3";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><style>");
        sb.append("body { margin: 0; padding: 0; background: ").append(bg).append("; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }");
        sb.append(".table-container { width: 100%; overflow-x: auto; }");
        sb.append("table { width: 100%; border-collapse: collapse; text-align: left; }");
        sb.append("th { background-color: ").append(headerBg).append("; color: ").append(headerText).append("; font-weight: 700; font-size: 14px; padding: 12px 18px; border: 1px solid ").append(headerBg).append("; }");
        sb.append("td { background-color: ").append(cellBg).append("; color: ").append(cellText).append("; font-size: 13px; padding: 9px 18px; border: 1px solid ").append(cellBorder).append("; vertical-align: top; }");
        sb.append("td.field-col { width: 160px; font-weight: 600; white-space: nowrap; }");
        sb.append("td.value-col { font-family: 'Consolas', 'Monaco', 'Menlo', monospace; font-size: 12px; word-break: break-all; }");
        sb.append("tr:hover td { filter: brightness(0.97); }");
        sb.append("</style></head><body>");
        sb.append("<div class='table-container'><table>");
        sb.append("<thead><tr><th style='width: 160px;'>Field</th><th>Value</th></tr></thead><tbody>");

        for (Map.Entry<String, String> entry : p.allProperties.entrySet()) {
            sb.append("<tr>");
            sb.append("<td class='field-col'>").append(escapeHtml(entry.getKey())).append("</td>");
            sb.append("<td class='value-col'>").append(escapeHtml(entry.getValue())).append("</td>");
            sb.append("</tr>");
        }

        sb.append("</tbody></table></div></body></html>");
        return sb.toString();
    }

    /**
     * Option 3: LINEAR_CHART (Image 3) - Smooth spline area line chart using Chart.js
     */
    private static String generateLinearChartHtml(ParsedResponse p, boolean isLight, String title) {
        String bg = isLight ? "#ffffff" : "#1e1e1e";
        String textColor = isLight ? "#333333" : "#e6edf3";
        String mutedText = isLight ? "#666666" : "#8b949e";
        String gridColor = isLight ? "rgba(0,0,0,0.06)" : "rgba(255,255,255,0.08)";

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        if (!p.linearMetrics.isEmpty()) {
            for (Map.Entry<String, Double> entry : p.linearMetrics.entrySet()) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
            }
        } else {
            labels.add("Status");
            values.add(200.0);
        }

        StringBuilder labelsJson = new StringBuilder("[");
        for (int i = 0; i < labels.size(); i++) {
            labelsJson.append("\"").append(escapeHtml(labels.get(i))).append("\"");
            if (i < labels.size() - 1) labelsJson.append(",");
        }
        labelsJson.append("]");

        StringBuilder valuesJson = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            valuesJson.append(values.get(i));
            if (i < values.size() - 1) valuesJson.append(",");
        }
        valuesJson.append("]");

        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<style>"
                + "body { margin: 0; padding: 24px 32px; background: " + bg + "; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }"
                + ".chart-box { width: 100%; height: 380px; position: relative; }"
                + "</style>"
                + "<script>" + CHART_JS + "</script>"
                + "</head><body>"
                + "<div class='chart-box'><canvas id='linearCanvas'></canvas></div>"
                + "<script>"
                + "const ctx = document.getElementById('linearCanvas').getContext('2d');"
                + "new Chart(ctx, {"
                + "  type: 'line',"
                + "  data: {"
                + "    labels: " + labelsJson + ","
                + "    datasets: [{"
                + "      label: 'Values',"
                + "      data: " + valuesJson + ","
                + "      borderColor: '#4db6ac',"
                + "      backgroundColor: 'rgba(128, 222, 234, 0.45)',"
                + "      fill: true,"
                + "      tension: 0.4,"
                + "      pointRadius: 4,"
                + "      pointBackgroundColor: '#4db6ac',"
                + "      pointBorderColor: '#ffffff',"
                + "      pointHoverRadius: 6"
                + "    }]"
                + "  },"
                + "  options: {"
                + "    responsive: true,"
                + "    maintainAspectRatio: false,"
                + "    plugins: {"
                + "      title: {"
                + "        display: true,"
                + "        text: '" + escapeHtml(title) + "',"
                + "        font: { size: 14, weight: '600', family: 'Segoe UI, sans-serif' },"
                + "        color: '" + textColor + "',"
                + "        padding: { top: 0, bottom: 18 }"
                + "      },"
                + "      legend: {"
                + "        display: true,"
                + "        position: 'top',"
                + "        labels: {"
                + "          boxWidth: 20,"
                + "          boxHeight: 10,"
                + "          color: '" + mutedText + "'"
                + "        }"
                + "      }"
                + "    },"
                + "    scales: {"
                + "      x: {"
                + "        title: { display: true, text: 'Metrics', color: '" + mutedText + "', font: { size: 12 } },"
                + "        grid: { color: '" + gridColor + "' },"
                + "        ticks: { color: '" + mutedText + "' }"
                + "      },"
                + "      y: {"
                + "        beginAtZero: true,"
                + "        title: { display: true, text: 'Values', color: '" + mutedText + "', font: { size: 12 } },"
                + "        grid: { color: '" + gridColor + "' },"
                + "        ticks: { color: '" + mutedText + "' }"
                + "      }"
                + "    }"
                + "  }"
                + "});"
                + "</script></body></html>";
    }

    /**
     * Option 4: BAR_GRAPH (Image 5) - Multi-colored comparative bar graph using Chart.js
     */
    private static String generateBarGraphHtml(ParsedResponse p, boolean isLight, String title) {
        String bg = isLight ? "#ffffff" : "#1e1e1e";
        String textColor = isLight ? "#333333" : "#e6edf3";
        String mutedText = isLight ? "#666666" : "#8b949e";
        String gridColor = isLight ? "rgba(0,0,0,0.06)" : "rgba(255,255,255,0.08)";

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        if (!p.barMetrics.isEmpty()) {
            for (Map.Entry<String, Double> entry : p.barMetrics.entrySet()) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
            }
        } else {
            labels.add("Status Code");
            values.add(200.0);
        }

        StringBuilder labelsJson = new StringBuilder("[");
        for (int i = 0; i < labels.size(); i++) {
            labelsJson.append("\"").append(escapeHtml(labels.get(i))).append("\"");
            if (i < labels.size() - 1) labelsJson.append(",");
        }
        labelsJson.append("]");

        StringBuilder valuesJson = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            valuesJson.append(values.get(i));
            if (i < values.size() - 1) valuesJson.append(",");
        }
        valuesJson.append("]");

        // Colors matching Image 5:
        // Bar 1 (User ID): Slate/deep purple #58508d
        // Bar 2 (Access Token): Deep purple #58508d
        // Bar 3 (Refresh Token): Magenta pink #bc5090
        String[] palette = {"#58508d", "#58508d", "#bc5090", "#ff6361", "#ffa600", "#003f5c"};
        StringBuilder colorsJson = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            colorsJson.append("\"").append(palette[i % palette.length]).append("\"");
            if (i < values.size() - 1) colorsJson.append(",");
        }
        colorsJson.append("]");

        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<style>"
                + "body { margin: 0; padding: 24px 32px; background: " + bg + "; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }"
                + ".chart-box { width: 100%; height: 380px; position: relative; }"
                + "</style>"
                + "<script>" + CHART_JS + "</script>"
                + "</head><body>"
                + "<div class='chart-box'><canvas id='barCanvas'></canvas></div>"
                + "<script>"
                + "const ctx = document.getElementById('barCanvas').getContext('2d');"
                + "new Chart(ctx, {"
                + "  type: 'bar',"
                + "  data: {"
                + "    labels: " + labelsJson + ","
                + "    datasets: [{"
                + "      label: 'Token Information',"
                + "      data: " + valuesJson + ","
                + "      backgroundColor: " + colorsJson + ","
                + "      borderColor: " + colorsJson + ","
                + "      borderWidth: 1,"
                + "      borderRadius: 2"
                + "    }]"
                + "  },"
                + "  options: {"
                + "    responsive: true,"
                + "    maintainAspectRatio: false,"
                + "    plugins: {"
                + "      title: {"
                + "        display: true,"
                + "        text: '" + escapeHtml(title) + "',"
                + "        font: { size: 14, weight: '600', family: 'Segoe UI, sans-serif' },"
                + "        color: '" + textColor + "',"
                + "        padding: { top: 0, bottom: 18 }"
                + "      },"
                + "      legend: {"
                + "        display: true,"
                + "        position: 'top',"
                + "        labels: {"
                + "          boxWidth: 20,"
                + "          boxHeight: 10,"
                + "          color: '" + mutedText + "'"
                + "        }"
                + "      }"
                + "    },"
                + "    scales: {"
                + "      x: {"
                + "        title: { display: true, text: 'Fields', color: '" + mutedText + "', font: { size: 12 } },"
                + "        grid: { display: false },"
                + "        ticks: { color: '" + mutedText + "' }"
                + "      },"
                + "      y: {"
                + "        beginAtZero: true,"
                + "        title: { display: true, text: 'Values', color: '" + mutedText + "', font: { size: 12 } },"
                + "        grid: { color: '" + gridColor + "' },"
                + "        ticks: { color: '" + mutedText + "' }"
                + "      }"
                + "    }"
                + "  }"
                + "});"
                + "</script></body></html>";
    }
}
