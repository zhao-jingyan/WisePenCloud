package com.oriole.wisepen.resource.util;

import java.util.regex.Pattern;

/**
 * ES 只需要可检索正文，页锚和 Markdown 结构标记在入库前统一清理。
 */
public final class SearchTextSanitizer {

    private static final Pattern HTML_COMMENT = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern FENCED_CODE_MARKER = Pattern.compile("(?m)^\\s*```.*$");
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*]\\([^)]*\\)");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern HEADING_PREFIX = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");
    private static final Pattern QUOTE_PREFIX = Pattern.compile("(?m)^\\s{0,3}>\\s?");
    private static final Pattern LIST_PREFIX = Pattern.compile("(?m)^\\s*(?:[-+*]|\\d+[.)])\\s+");
    private static final Pattern TABLE_SEPARATOR_LINE = Pattern.compile("(?m)^\\s*\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
    private static final Pattern MARKDOWN_CONTROL_CHARS = Pattern.compile("[`*_~#>|\\[\\]()]");
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("(?m)^\\s*[-*_]{3,}\\s*$");
    private static final Pattern BLANK_AROUND_LINE_BREAK = Pattern.compile("[ \\t]*\\R[ \\t]*");
    private static final Pattern REPEATED_SPACE = Pattern.compile("[ \\t]{2,}");
    private static final Pattern REPEATED_LINE_BREAK = Pattern.compile("\\R{3,}");

    private SearchTextSanitizer() {
    }

    public static String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String sanitized = text.replace("\r\n", "\n").replace('\r', '\n');
        sanitized = HTML_COMMENT.matcher(sanitized).replaceAll("\n");
        sanitized = FENCED_CODE_MARKER.matcher(sanitized).replaceAll("\n");
        sanitized = MARKDOWN_IMAGE.matcher(sanitized).replaceAll(" ");
        sanitized = MARKDOWN_LINK.matcher(sanitized).replaceAll("$1");
        sanitized = TABLE_SEPARATOR_LINE.matcher(sanitized).replaceAll("\n");
        sanitized = HEADING_PREFIX.matcher(sanitized).replaceAll("");
        sanitized = QUOTE_PREFIX.matcher(sanitized).replaceAll("");
        sanitized = LIST_PREFIX.matcher(sanitized).replaceAll("");
        sanitized = HORIZONTAL_RULE.matcher(sanitized).replaceAll("\n");
        sanitized = sanitized.replace('|', ' ');
        sanitized = MARKDOWN_CONTROL_CHARS.matcher(sanitized).replaceAll("");
        sanitized = BLANK_AROUND_LINE_BREAK.matcher(sanitized).replaceAll("\n");
        sanitized = REPEATED_SPACE.matcher(sanitized).replaceAll(" ");
        sanitized = REPEATED_LINE_BREAK.matcher(sanitized).replaceAll("\n\n");
        return sanitized.trim();
    }
}
