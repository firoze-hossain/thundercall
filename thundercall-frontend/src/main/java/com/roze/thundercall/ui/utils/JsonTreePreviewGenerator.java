package com.roze.thundercall.ui.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

/**
 * Generates Postman-style interactive hierarchical JSON Tree/Table Preview HTML.
 * Matches Postman's Preview tab:
 *  - Key column on the left with collapsible chevrons and child count badges ({N} or [N]).
 *  - Value column on the right with custom styled types: null pill, boolean, number, string.
 *  - Interactive click-to-expand/collapse on nested objects/arrays.
 *  - Preserves exact JSON field insertion order using Jackson JsonNode.
 *  - Clean row divider lines and subtle hover effects.
 *  - Supports both Light and Dark themes.
 */
public final class JsonTreePreviewGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTreePreviewGenerator() {}

    public static String generateHtml(String jsonString, boolean isLight) {
        if (jsonString == null || jsonString.isBlank()) {
            return generateEmptyHtml(isLight);
        }

        String bg = isLight ? "#ffffff" : "#1d1f24";
        String text = isLight ? "#24292f" : "#e6edf3";
        String border = isLight ? "#f0f0f0" : "#282e38";
        String hoverBg = isLight ? "#f8fafc" : "#22272e";
        String keyColor = isLight ? "#212529" : "#e6edf3";
        String chevronColor = isLight ? "#8c9199" : "#8b949e";
        String chevronHoverColor = isLight ? "#212529" : "#ffffff";
        String badgeBg = isLight ? "#f0f2f5" : "#262c36";
        String badgeBorder = isLight ? "#d0d7de" : "#363d47";
        String badgeText = isLight ? "#57606a" : "#8b949e";
        String nullBg = isLight ? "#f1f3f5" : "#282e38";
        String nullBorder = isLight ? "#e1e4e8" : "#363d47";
        String nullText = isLight ? "#6e7781" : "#8b949e";
        String boolColor = isLight ? "#005cc5" : "#79c0ff";
        String numColor = isLight ? "#098658" : "#7ee787";
        String strColor = isLight ? "#24292f" : "#c9d1d9";
        String indentLine = isLight ? "#e8e8e8" : "#30363d";

        StringBuilder bodyHtml = new StringBuilder();

        try {
            JsonNode root = MAPPER.readTree(jsonString.trim());
            if (root.isObject()) {
                renderObjectChildren(root, bodyHtml);
            } else if (root.isArray()) {
                renderArrayChildren(root, bodyHtml);
            } else {
                bodyHtml.append("<div class='tree-row primitive-row'><div class='tree-key-col'><span class='key-name'>response</span></div><div class='tree-val-col'><span class='val-string'>")
                        .append(escapeHtml(root.asText())).append("</span></div></div>");
            }
        } catch (Exception e) {
            return "<!DOCTYPE html><html><body style='font-family:sans-serif;padding:24px;background:" + bg + ";color:" + text + ";'>"
                    + "<h3>Unable to parse JSON for Preview</h3><pre style='font-family:monospace;white-space:pre-wrap;'>"
                    + escapeHtml(jsonString) + "</pre></body></html>";
        }

        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n<style>\n"
                + "* { box-sizing: border-box; margin: 0; padding: 0; }\n"
                + "body {\n"
                + "  background-color: " + bg + ";\n"
                + "  color: " + text + ";\n"
                + "  font-family: 'Consolas', 'Menlo', 'JetBrains Mono', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n"
                + "  font-size: 12px;\n"
                + "  line-height: 1.5;\n"
                + "  padding: 8px 16px;\n"
                + "  overflow-x: auto;\n"
                + "}\n"
                + ".tree-container { width: 100%; min-width: 480px; }\n"
                + ".tree-row {\n"
                + "  display: flex;\n"
                + "  align-items: flex-start;\n"
                + "  min-height: 29px;\n"
                + "  padding: 5px 4px 5px 6px;\n"
                + "  border-bottom: 1px solid " + border + ";\n"
                + "  transition: background-color 0.1s ease;\n"
                + "  border-radius: 4px;\n"
                + "}\n"
                + ".tree-row:hover { background-color: " + hoverBg + "; }\n"
                + ".tree-row.object-row { cursor: pointer; user-select: none; }\n"
                + ".tree-key-col {\n"
                + "  width: 220px;\n"
                + "  min-width: 140px;\n"
                + "  max-width: 320px;\n"
                + "  flex-shrink: 0;\n"
                + "  display: flex;\n"
                + "  align-items: center;\n"
                + "  padding-right: 16px;\n"
                + "}\n"
                + ".key-name {\n"
                + "  font-weight: 600;\n"
                + "  color: " + keyColor + ";\n"
                + "  overflow: hidden;\n"
                + "  text-overflow: ellipsis;\n"
                + "  white-space: nowrap;\n"
                + "}\n"
                + ".tree-chevron {\n"
                + "  display: inline-flex;\n"
                + "  align-items: center;\n"
                + "  justify-content: center;\n"
                + "  width: 16px;\n"
                + "  height: 16px;\n"
                + "  margin-right: 4px;\n"
                + "  color: " + chevronColor + ";\n"
                + "  flex-shrink: 0;\n"
                + "}\n"
                + ".tree-row:hover .tree-chevron { color: " + chevronHoverColor + "; }\n"
                + ".tree-badge {\n"
                + "  background: " + badgeBg + ";\n"
                + "  border: 1px solid " + badgeBorder + ";\n"
                + "  color: " + badgeText + ";\n"
                + "  font-size: 11px;\n"
                + "  font-weight: bold;\n"
                + "  padding: 0 5px;\n"
                + "  border-radius: 3px;\n"
                + "  margin-left: 6px;\n"
                + "  line-height: 16px;\n"
                + "  height: 18px;\n"
                + "  display: inline-block;\n"
                + "}\n"
                + ".tree-val-col {\n"
                + "  flex-grow: 1;\n"
                + "  min-width: 0;\n"
                + "  word-break: break-all;\n"
                + "  overflow-wrap: break-word;\n"
                + "  font-family: 'Consolas', 'JetBrains Mono', monospace;\n"
                + "}\n"
                + ".val-null {\n"
                + "  background: " + nullBg + ";\n"
                + "  border: 1px solid " + nullBorder + ";\n"
                + "  color: " + nullText + ";\n"
                + "  font-size: 11px;\n"
                + "  font-weight: 500;\n"
                + "  padding: 1px 7px;\n"
                + "  border-radius: 4px;\n"
                + "  display: inline-block;\n"
                + "  line-height: 1.3;\n"
                + "}\n"
                + ".val-boolean { color: " + boolColor + "; font-weight: 600; }\n"
                + ".val-number { color: " + numColor + "; font-weight: normal; }\n"
                + ".val-string { color: " + strColor + "; font-weight: normal; }\n"
                + ".tree-children {\n"
                + "  padding-left: 20px;\n"
                + "  border-left: 1px solid " + indentLine + ";\n"
                + "  margin-left: 7px;\n"
                + "}\n"
                + "</style>\n"
                + "<script>\n"
                + "function toggleNode(row) {\n"
                + "  var node = row.parentElement;\n"
                + "  var children = node.querySelector(':scope > .tree-children');\n"
                + "  var chevron = row.querySelector('.chevron-icon');\n"
                + "  if (!children) return;\n"
                + "  var isHidden = children.style.display === 'none';\n"
                + "  if (isHidden) {\n"
                + "    children.style.display = 'block';\n"
                + "    if (chevron) chevron.innerHTML = '<path d=\"M2.5 4.5L6 8L9.5 4.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>';\n"
                + "  } else {\n"
                + "    children.style.display = 'none';\n"
                + "    if (chevron) chevron.innerHTML = '<path d=\"M4.5 2.5L8 6L4.5 9.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>';\n"
                + "  }\n"
                + "}\n"
                + "function expandAll() {\n"
                + "  document.querySelectorAll('.tree-children').forEach(function(el) { el.style.display = 'block'; });\n"
                + "  document.querySelectorAll('.chevron-icon').forEach(function(el) { el.innerHTML = '<path d=\"M2.5 4.5L6 8L9.5 4.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>'; });\n"
                + "}\n"
                + "function collapseAll() {\n"
                + "  document.querySelectorAll('.tree-children').forEach(function(el) { el.style.display = 'none'; });\n"
                + "  document.querySelectorAll('.chevron-icon').forEach(function(el) { el.innerHTML = '<path d=\"M4.5 2.5L8 6L4.5 9.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>'; });\n"
                + "}\n"
                + "</script>\n"
                + "</head>\n<body>\n"
                + "<div class='tree-container'>\n"
                + bodyHtml.toString()
                + "</div>\n</body>\n</html>";
    }

    private static void renderObjectChildren(JsonNode node, StringBuilder sb) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            renderNode(entry.getKey(), entry.getValue(), sb);
        }
    }

    private static void renderArrayChildren(JsonNode arr, StringBuilder sb) {
        for (int i = 0; i < arr.size(); i++) {
            renderNode("[" + i + "]", arr.get(i), sb);
        }
    }

    private static void renderNode(String key, JsonNode val, StringBuilder sb) {
        if (val == null || val.isNull()) {
            sb.append("<div class='tree-row primitive-row'>")
                    .append("<div class='tree-key-col'><span class='key-name'>").append(escapeHtml(key)).append("</span></div>")
                    .append("<div class='tree-val-col'><span class='val-null'>null</span></div>")
                    .append("</div>\n");
        } else if (val.isBoolean()) {
            sb.append("<div class='tree-row primitive-row'>")
                    .append("<div class='tree-key-col'><span class='key-name'>").append(escapeHtml(key)).append("</span></div>")
                    .append("<div class='tree-val-col'><span class='val-boolean'>").append(val.asBoolean()).append("</span></div>")
                    .append("</div>\n");
        } else if (val.isNumber()) {
            sb.append("<div class='tree-row primitive-row'>")
                    .append("<div class='tree-key-col'><span class='key-name'>").append(escapeHtml(key)).append("</span></div>")
                    .append("<div class='tree-val-col'><span class='val-number'>").append(val.asText()).append("</span></div>")
                    .append("</div>\n");
        } else if (val.isTextual()) {
            sb.append("<div class='tree-row primitive-row'>")
                    .append("<div class='tree-key-col'><span class='key-name'>").append(escapeHtml(key)).append("</span></div>")
                    .append("<div class='tree-val-col'><span class='val-string'>").append(escapeHtml(val.asText())).append("</span></div>")
                    .append("</div>\n");
        } else if (val.isObject()) {
            int count = val.size();
            sb.append("<div class='tree-node'>")
                    .append("<div class='tree-row object-row' onclick='toggleNode(this)'>")
                    .append("<div class='tree-key-col'>")
                    .append("<span class='tree-chevron'><svg class='chevron-icon' width='12' height='12' viewBox='0 0 12 12'><path d='M4.5 2.5L8 6L4.5 9.5' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/></svg></span>")
                    .append("<span class='key-name'>").append(escapeHtml(key)).append("</span>")
                    .append("<span class='tree-badge'>{").append(count).append("}</span>")
                    .append("</div>")
                    .append("<div class='tree-val-col'></div>")
                    .append("</div>")
                    .append("<div class='tree-children' style='display:none;'>\n");
            renderObjectChildren(val, sb);
            sb.append("</div></div>\n");
        } else if (val.isArray()) {
            int count = val.size();
            sb.append("<div class='tree-node'>")
                    .append("<div class='tree-row object-row' onclick='toggleNode(this)'>")
                    .append("<div class='tree-key-col'>")
                    .append("<span class='tree-chevron'><svg class='chevron-icon' width='12' height='12' viewBox='0 0 12 12'><path d='M4.5 2.5L8 6L4.5 9.5' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/></svg></span>")
                    .append("<span class='key-name'>").append(escapeHtml(key)).append("</span>")
                    .append("<span class='tree-badge'>[").append(count).append("]</span>")
                    .append("</div>")
                    .append("<div class='tree-val-col'></div>")
                    .append("</div>")
                    .append("<div class='tree-children' style='display:none;'>\n");
            renderArrayChildren(val, sb);
            sb.append("</div></div>\n");
        } else {
            sb.append("<div class='tree-row primitive-row'>")
                    .append("<div class='tree-key-col'><span class='key-name'>").append(escapeHtml(key)).append("</span></div>")
                    .append("<div class='tree-val-col'><span class='val-string'>").append(escapeHtml(val.asText())).append("</span></div>")
                    .append("</div>\n");
        }
    }

    private static String generateEmptyHtml(boolean isLight) {
        String bg = isLight ? "#ffffff" : "#1d1f24";
        String textMuted = isLight ? "#57606a" : "#8b949e";
        return "<!DOCTYPE html><html><body style='font-family:sans-serif;padding:24px;text-align:center;background:"
                + bg + ";color:" + textMuted + ";'>"
                + "<h3>No JSON data to preview</h3><p>Send a request returning JSON to preview its structure.</p></body></html>";
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
