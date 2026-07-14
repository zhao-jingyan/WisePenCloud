package com.oriole.wisepen.document.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class MarkdownPageBreakInjector {

    private static final int MIN_ANCHOR_LENGTH = 24;
    private static final int[] ANCHOR_LENGTH_CANDIDATES = {160, 120, 80, 48, 32, 24};

    public String inject(String markdown, File pdfFile, String documentId) {
        if (markdown == null || markdown.isBlank()) {
            return markdown; // 检查 Markdown 是否为空，空则原样返回
        }

        List<String> pdfPageTexts = extractPdfPageTexts(pdfFile, documentId);
        if (pdfPageTexts.isEmpty()) {
            return markdown;
        }
        if (pdfPageTexts.size() == 1) {
            return pageStartMarker(1) + "\n\n" + markdown.trim() + "\n\n" + pageEndMarker(1);
        }

        NormalizedText normalizedMarkdown = normalizeWithOffsets(markdown); // 归一化，并保留归一化字符 -> 原 Markdown offset的映射。
        if (normalizedMarkdown.value().isBlank()) {
            return markdown;
        }

        List<Insertion> insertions = new ArrayList<>();
        int searchStart = 0; // 顺序匹配
        int skipped = 0;
        for (int pageIndex = 0; pageIndex < pdfPageTexts.size() - 1; pageIndex++) {
            String normalizedPageText = normalize(pdfPageTexts.get(pageIndex));
            Match match = findPageEnd(normalizedMarkdown.value(), normalizedPageText, searchStart);
            if (match == null) {
                skipped++;
                continue;
            }

            int boundaryIndex = match.start() + match.length();
            int originalOffset = toOriginalOffset(normalizedMarkdown, markdown.length(), boundaryIndex);
            insertions.add(new Insertion(alignToBlockBoundary(markdown, originalOffset), pageIndex + 2));
            searchStart = boundaryIndex; // 保证后续页只会在前一页之后查找
        }

        if (insertions.isEmpty()) {
            log.warn("markdown page break injection skipped. documentId={} source={} pages={} reason=\"no page anchor matched\"",
                    documentId, pdfFile.getName(), pdfPageTexts.size());
            return markdown;
        }
        if (skipped > 0) {
            log.warn("markdown page break injection partially skipped. documentId={} source={} pages={} injected={} skipped={}",
                    documentId, pdfFile.getName(), pdfPageTexts.size(), insertions.size(), skipped);
        } else {
            log.debug("markdown page break injection finished. documentId={} source={} pages={} injected={}",
                    documentId, pdfFile.getName(), pdfPageTexts.size(), insertions.size());
        }

        return applyInsertions(markdown, insertions, pdfPageTexts.size());
    }

    // 页起始标记格式
    public static String pageStartMarker(int pageNo) {
        return "<!-- page:start page=" + pageNo + " -->";
    }

    // 页结束标记格式
    public static String pageEndMarker(int pageNo) {
        return "<!-- page:end page=" + pageNo + " -->";
    }

    // PDF 逐页抽取
    private List<String> extractPdfPageTexts(File pdfFile, String documentId) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 按页面视觉位置输出文本

            int pageCount = document.getNumberOfPages();
            List<String> pageTexts = new ArrayList<>(pageCount);
            for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);
                pageTexts.add(stripper.getText(document));
            }
            return pageTexts;
        } catch (IOException e) {
            log.warn("markdown page break injection skipped. documentId={} source={} reason=\"pdf text read failed\"",
                    documentId, pdfFile.getName(), e);
            return List.of();
        }
    }

    // 基于 PDF 当前页的末尾小段文本，在 Markdown 中查找后插入页边界
    private Match findPageEnd(String normalizedMarkdown, String normalizedPageText, int searchStart) {
        if (normalizedPageText.length() < MIN_ANCHOR_LENGTH) {
            return null;
        }

        int previousAnchorLength = -1;
        // 锚点长度候选，依次尝试若干归一化字符，找不到即缩短，避免误匹配到文中重复片段
        for (int candidateLength : ANCHOR_LENGTH_CANDIDATES) {
            int anchorLength = Math.min(candidateLength, normalizedPageText.length());
            if (anchorLength < MIN_ANCHOR_LENGTH || anchorLength == previousAnchorLength) {
                continue;
            }
            previousAnchorLength = anchorLength;

            String anchor = normalizedPageText.substring(normalizedPageText.length() - anchorLength);
            int matchStart = normalizedMarkdown.indexOf(anchor, searchStart);
            if (matchStart >= 0) {
                return new Match(matchStart, anchor.length());
            }
        }
        return null;
    }

    private String applyInsertions(String markdown, List<Insertion> insertions, int pageCount) {
        // 从前往后插入，第一个分页符会改变后面所有 offset，导致后续插错位置
        // 倒序插入可以避免 offset 位移问题
        StringBuilder result = new StringBuilder(markdown);
        insertions.stream()
                .sorted(Comparator.comparingInt(Insertion::offset).reversed())
                .forEach(insertion -> result.insert(
                        insertion.offset(),
                        "\n\n" + pageEndMarker(insertion.pageNo() - 1)
                                + "\n\n" + pageStartMarker(insertion.pageNo()) + "\n\n"));
        return (pageStartMarker(1) + "\n\n" + result.toString().trim() + "\n\n" + pageEndMarker(pageCount)).trim();
    }

    private static int toOriginalOffset(NormalizedText normalizedText, int markdownLength, int normalizedIndex) {
        if (normalizedIndex <= 0) {
            return 0;
        }
        if (normalizedIndex >= normalizedText.originalOffsets().length) {
            return markdownLength;
        }
        return normalizedText.originalOffsets()[normalizedIndex - 1] + 1; // 从归一化位置回到原文位置
    }

    private static int alignToBlockBoundary(String markdown, int offset) {
        // 匹配位置可能落在段落中间或行尾附近，应从当前 offset 往后走到本行结束再跳过后续空白，返回一个更适合插入块级标记的位置
        int cursor = Math.min(Math.max(offset, 0), markdown.length());
        while (cursor < markdown.length() && markdown.charAt(cursor) != '\n' && markdown.charAt(cursor) != '\r') {
            cursor++;
        }
        while (cursor < markdown.length() && Character.isWhitespace(markdown.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static NormalizedText normalizeWithOffsets(String text) {
        // 匹配发生在归一化字符串里，但插入分页符必须插回原始 Markdown，需要记录每个归一化字符来自原 Markdown 的位置
        StringBuilder normalized = new StringBuilder(text.length());
        int[] offsets = new int[text.length()];
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char normalizedChar = normalizeChar(text.charAt(i));
            if (normalizedChar == 0) {
                continue;
            }
            normalized.append(normalizedChar);
            offsets[count++] = i;
        }
        return new NormalizedText(normalized.toString(), Arrays.copyOf(offsets, count));
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char normalizedChar = normalizeChar(text.charAt(i));
            if (normalizedChar != 0) {
                normalized.append(normalizedChar);
            }
        }
        return normalized.toString();
    }

    // 去掉空白和控制字符；去掉常见 Markdown 标记字符；其他字符转小写
    private static char normalizeChar(char ch) {
        if (Character.isWhitespace(ch) || Character.isISOControl(ch)) {
            return 0;
        }
        return switch (ch) {
            case '#', '*', '_', '`', '>', '|', '[', ']', '(', ')', '!', '\\', '-', '+', '=', '~', '<', '/', '{', '}' -> 0;
            default -> Character.toLowerCase(ch);
        };
    }

    private record NormalizedText(String value, int[] originalOffsets) {
    }

    private record Match(int start, int length) {
    }

    private record Insertion(int offset, int pageNo) {
    }
}
