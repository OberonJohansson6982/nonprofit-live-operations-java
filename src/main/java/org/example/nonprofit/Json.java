package org.example.nonprofit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(String.valueOf(entry.getKey()))).append(':').append(write(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) out.append(',');
                first = false;
                out.append(write(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    static Object read(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (parser.index != source.length()) throw new IllegalArgumentException("Trailing JSON input");
        return value;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) { this.source = source; }

        private Object value() {
            space();
            if (index >= source.length()) throw new IllegalArgumentException("Missing JSON value");
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            do {
                space();
                String key = string();
                space();
                expect(':');
                result.put(key, value());
                space();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            index++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            do {
                result.add(value());
                space();
            } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('\"');
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '\"') return result.toString();
                if (c != '\\') { result.append(c); continue; }
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '\"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        result.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }

        private Object number() {
            int start = index;
            while (index < source.length() && "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) index++;
            String token = source.substring(start, index);
            try {
                return token.contains(".") || token.contains("e") || token.contains("E")
                        ? Double.parseDouble(token) : Long.parseLong(token);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid JSON number", exception);
            }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, index)) throw new IllegalArgumentException("Invalid JSON literal");
            index += token.length();
            return value;
        }

        private void space() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        }

        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) { index++; return true; }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected '" + expected + "'");
        }
    }
}
