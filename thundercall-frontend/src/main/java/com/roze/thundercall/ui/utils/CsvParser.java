package com.roze.thundercall.ui.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free RFC4180-ish CSV reader — just enough to import a
 * "requests spreadsheet" (columns: Folder, Name, Method, URL, Headers, Body,
 * PreRequestScript, TestsScript). Handles quoted fields, embedded commas,
 * embedded newlines inside quotes, and "" as an escaped quote.
 */
public final class CsvParser {

    private CsvParser() {
    }

    /** Parses the whole file into a list of rows, each a String[] of cells. */
    public static List<String[]> parse(String csvText) {
        List<String[]> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int n = csvText.length();
        // Strip a UTF-8 BOM if present (common in Excel/Postman exports)
        if (n > 0 && csvText.charAt(0) == '\uFEFF') {
            i = 1;
        }
        while (i < n) {
            char c = csvText.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && csvText.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                } else if (c == '\r') {
                    // ignore; \n (or end) will close the row
                } else if (c == '\n') {
                    current.add(field.toString());
                    field.setLength(0);
                    rows.add(current.toArray(new String[0]));
                    current = new ArrayList<>();
                } else {
                    field.append(c);
                }
            }
            i++;
        }
        // Last field/row if the file doesn't end with a newline
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current.toArray(new String[0]));
        }
        return rows;
    }

    /** Parses with the first row as a header, returning List of column->value maps. */
    public static List<Map<String, String>> parseWithHeader(String csvText) {
        List<String[]> rows = parse(csvText);
        List<Map<String, String>> result = new ArrayList<>();
        if (rows.isEmpty()) {
            return result;
        }
        String[] header = rows.get(0);
        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            if (row.length == 1 && row[0].isBlank()) {
                continue; // skip trailing blank lines
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (int c = 0; c < header.length; c++) {
                map.put(header[c].trim(), c < row.length ? row[c] : "");
            }
            result.add(map);
        }
        return result;
    }
}