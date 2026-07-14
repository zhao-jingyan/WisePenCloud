package com.oriole.wisepen.document.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 面向搜索文本的轻量清洗 PDFBox 输出
 */
public final class PdfTextNormalizer {

    private PdfTextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalizedLineBreaks = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedLineBreaks.split("\n", -1);
        List<String> paragraphs = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = rtrim(line).trim();
            if (trimmedLine.isEmpty()) {
                // 空行视为段落边界，连续空行会被压缩成一个段落分隔
                appendParagraph(paragraphs, paragraph);
                continue;
            }

            if (paragraph.length() == 0) {
                paragraph.append(trimmedLine);
                continue;
            }
            // 非空行默认视为同一段里的视觉换行，由 appendLine 决定拼接方式
            appendLine(paragraph, trimmedLine);
        }
        appendParagraph(paragraphs, paragraph);

        return String.join("\n\n", paragraphs).trim();
    }

    private static void appendParagraph(List<String> paragraphs, StringBuilder paragraph) {
        if (paragraph.length() == 0) {
            return;
        }
        paragraphs.add(paragraph.toString().trim());
        paragraph.setLength(0);
    }

    private static void appendLine(StringBuilder paragraph, String line) {
        char previous = paragraph.charAt(paragraph.length() - 1);
        char next = line.charAt(0);

        // 英文 PDF 常见断词，例如 trans-\nformer，应合并为 transformer
        if (isHyphenatedWordBreak(paragraph, next)) {
            paragraph.deleteCharAt(paragraph.length() - 1);
            paragraph.append(line);
            return;
        }
        // 中文、日文、韩文行间通常不需要补空格
        if (isCjk(previous) && isCjk(next)) {
            paragraph.append(line);
            return;
        }

        if (!Character.isWhitespace(previous)) {
            paragraph.append(' ');
        }
        paragraph.append(line);
    }

    private static String rtrim(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean isHyphenatedWordBreak(StringBuilder paragraph, char next) {
        int length = paragraph.length();
        return length >= 2
                && paragraph.charAt(length - 1) == '-'
                && isAsciiLetter(paragraph.charAt(length - 2))
                && next >= 'a'
                && next <= 'z';
    }

    private static boolean isAsciiLetter(char value) {
        return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z');
    }

    private static boolean isCjk(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }
}
