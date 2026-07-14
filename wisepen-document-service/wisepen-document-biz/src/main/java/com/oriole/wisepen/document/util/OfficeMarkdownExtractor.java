package com.oriole.wisepen.document.util;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.document.exception.DocumentError;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OfficeMarkdownExtractor {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public String extract(File source, ResourceType fileType) {
        try {
            return switch (fileType) {
                case DOCX -> {
                    try (FileInputStream inputStream = new FileInputStream(source)) {
                        yield extractDocx(new XWPFDocument(inputStream));
                    }
                }
                case DOC -> {
                    try (FileInputStream inputStream = new FileInputStream(source)) {
                        yield extractDoc(new HWPFDocument(inputStream));
                    }
                }
                case PPTX -> {
                    try (FileInputStream inputStream = new FileInputStream(source)) {
                        yield extractSlideShow(new XMLSlideShow(inputStream));
                    }
                }
                case PPT -> {
                    try (FileInputStream inputStream = new FileInputStream(source)) {
                        yield extractSlideShow(new HSLFSlideShow(inputStream));
                    }
                }
                case XLSX -> {
                    try (FileInputStream inputStream = new FileInputStream(source)) {
                        yield extractWorkbook(new XSSFWorkbook(inputStream));
                    }
                }
                case XLS -> {
                    try (FileInputStream inputStream = new FileInputStream(source)) {
                        yield extractWorkbook(new HSSFWorkbook(inputStream));
                    }
                }
                default -> "";
            };
        } catch (Exception e) {
            log.error("office markdown extraction failed. source={} fileType={}", source.getName(), fileType, e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_MARKDOWN_FAILED, e.getMessage());
        }
    }

    private String extractDocx(XWPFDocument document) throws Exception {
        try (document) {
            StringBuilder markdown = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraph(markdown, paragraph.getText());
                } else if (element instanceof XWPFTable table) {
                    appendDocxTable(markdown, table);
                }
            }
            return markdown.toString().trim();
        }
    }

    private String extractDoc(HWPFDocument document) throws Exception {
        try (document; WordExtractor extractor = new WordExtractor(document)) {
            StringBuilder markdown = new StringBuilder();
            for (String paragraph : extractor.getParagraphText()) {
                appendParagraph(markdown, paragraph);
            }
            return markdown.toString().trim();
        }
    }

    private void appendDocxTable(StringBuilder markdown, XWPFTable table) {
        List<List<String>> rows = new ArrayList<>();
        int maxColumns = 0;
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = row.getTableCells().stream()
                    .map(cell -> escapeTableCell(cell.getText()))
                    .toList();
            if (cells.stream().allMatch(String::isBlank)) {
                continue;
            }
            rows.add(new ArrayList<>(cells));
            maxColumns = Math.max(maxColumns, cells.size());
        }
        if (rows.isEmpty()) {
            return;
        }
        appendMarkdownTable(markdown, rows, maxColumns);
    }

    private <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>>
    String extractSlideShow(SlideShow<S, P> slideShow) throws Exception {
        try (slideShow) {
            StringBuilder markdown = new StringBuilder();
            int slideNo = 1;

            // 每一页 slide 输出为一个二级标题，保留幻灯片分页结构
            for (Slide<S, P> slide : slideShow.getSlides()) {
                if (markdown.length() > 0) {
                    markdown.append("\n\n");
                }
                markdown.append(MarkdownPageBreakInjector.pageStartMarker(slideNo)).append("\n\n");
                markdown.append("## Slide ").append(slideNo).append("\n\n");

                // 遍历当前 slide 上的所有图形对象
                for (S shape : slide.getShapes()) {
                    // 这里只抽取文本框，图片、图表等暂不处理
                    if (shape instanceof TextShape<?, ?> textShape) {
                        appendParagraph(markdown, textShape.getText());
                    }
                }
                markdown.append(MarkdownPageBreakInjector.pageEndMarker(slideNo)).append("\n\n");
                slideNo++;
            }
            return markdown.toString().trim();
        }
    }

    private String extractWorkbook(Workbook workbook) throws Exception {
        try (workbook) {
            StringBuilder markdown = new StringBuilder();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                markdown.append("## ").append(escapeMarkdownText(sheet.getSheetName())).append("\n\n");
                List<List<String>> rows = new ArrayList<>();
                int maxColumns = 0;

                // 遍历 sheet 中实际存在的行
                for (Row row : sheet) {
                    List<String> values = new ArrayList<>();

                    // getLastCellNum 返回“最后一个单元格下标 + 1”
                    // 如果该行没有单元格，返回 -1
                    short lastCellNum = row.getLastCellNum();
                    if (lastCellNum < 0) {
                        continue;
                    }

                    // 从第 0 列遍历到最后一列，保留中间空单元格
                    for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        values.add(escapeTableCell(cell == null ? "" : DATA_FORMATTER.formatCellValue(cell)));
                    }

                    // 全空行不进入 Markdown
                    if (values.stream().allMatch(String::isBlank)) {
                        continue;
                    }
                    maxColumns = Math.max(maxColumns, values.size());
                    rows.add(values);
                }
                if (rows.isEmpty()) {
                    continue;
                }
                appendMarkdownTable(markdown, rows, maxColumns);
            }
            return markdown.toString().trim();
        }
    }

    private static void appendParagraph(StringBuilder markdown, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\\R+")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                markdown.append(trimmed).append("\n\n");
            }
        }
    }

    private static void appendMarkdownTable(StringBuilder markdown, List<List<String>> rows, int maxColumns){
        // Markdown 表格要求每一行列数一致，短行补空字符串
        for (List<String> row : rows) {
            while (row.size() < maxColumns) {
                row.add("");
            }
        }
        // 二维数组写成 Markdown 表格
        // 第一行作为表头
        List<String> header = rows.getFirst();
        markdown.append("| ").append(String.join(" | ", header)).append(" |\n");
        // Markdown 表格分隔行
        markdown.append("| ").append(String.join(" | ", header.stream().map(ignored -> "---").toList())).append(" |\n");
        // 后续行作为数据行
        for (int i = 1; i < rows.size(); i++) {
            markdown.append("| ").append(String.join(" | ", rows.get(i))).append(" |\n");
        }
        markdown.append("\n");
    }


    private static String escapeTableCell(String value) {
        // 竖线 | 是 Markdown 表格列分隔符需要转义
        return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", " ").replace("\n", "<br>");
    }

    private static String escapeMarkdownText(String value) {
        // sheet 名会作为 Markdown 标题输出，如果 sheet 名里有 # 需要转义
        return value == null ? "" : value.replace("\\", "\\\\").replace("#", "\\#");
    }
}
