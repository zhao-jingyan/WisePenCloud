package com.oriole.wisepen.document.service.impl;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.document.exception.DocumentError;
import com.oriole.wisepen.document.service.IDocumentFileService;
import com.oriole.wisepen.document.util.OfficeMarkdownExtractor;
import com.oriole.wisepen.document.util.OnlyOfficeConversionClient;
import com.oriole.wisepen.document.util.PdfTextNormalizer;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static com.oriole.wisepen.document.util.MarkdownPageBreakInjector.pageEndMarker;
import static com.oriole.wisepen.document.util.MarkdownPageBreakInjector.pageStartMarker;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFileServiceImpl implements IDocumentFileService {

    private final DocumentConverter documentConverter;
    private final OnlyOfficeConversionClient onlyOfficeConversionClient;
    private final OfficeMarkdownExtractor officeMarkdownExtractor;

    @Override
    public void convertTo(File source, File target) {
        long start = System.currentTimeMillis();
        log.info("legacy office to pdf conversion started. source={} size={}", source.getName(), source.length());
        try {
            documentConverter.convert(source).to(target).execute();
            log.info("legacy office to pdf conversion finished. costMs={}", System.currentTimeMillis() - start);
        } catch (OfficeException e) {
            log.error("legacy office to pdf conversion failed. costMs={} source={}", System.currentTimeMillis() - start, source.getName(), e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_CONVERT_FAILED);
        }
    }

    @Override
    public void convertTo(String sourceUrl, String sourceName, ResourceType sourceType, File target, OnlyOfficeConversionClient.ConversionTargetType targetType) {
        long start = System.currentTimeMillis();
        log.info("onlyoffice pdf conversion started. sourceName={} sourceType={} targetType={}", sourceName, sourceType, targetType);
        try {
            onlyOfficeConversionClient.convert(sourceUrl, sourceType, targetType, target);
            log.info("onlyoffice conversion finished. costMs={}", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("onlyoffice office  conversion failed. costMs={} sourceName={}", System.currentTimeMillis() - start, sourceName, e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_CONVERT_FAILED);
        }
    }

    @Override
    public String extractMarkdown(File source, ResourceType fileType) {
        if (!Set.of(ResourceType.DOC, ResourceType.DOCX, ResourceType.PPT, ResourceType.PPTX, ResourceType.XLS, ResourceType.XLSX).contains(fileType)) {
            throw new ServiceException(DocumentError.CANNOT_SUPPORT_FILE_TYPE);
        }
        try {
            String markdown = officeMarkdownExtractor.extract(source, fileType);
            log.debug("document markdown extraction finished.");
            return markdown;
        } catch (Exception e) {
            log.error("document markdown extraction failed. source={} fileType={}", source.getName(), fileType, e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_MARKDOWN_FAILED, e.getMessage());
        }
    }

    @Override
    public String extractPDFText(File pdfFile) {
        log.debug("pdf text extraction started. source={}", pdfFile.getName());
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            int pageCount = doc.getNumberOfPages();
            int charCount = 0;
            StringBuilder normalizedText = new StringBuilder();
            for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);
                String pageText = stripper.getText(doc);
                charCount += pageText.length();

                pageText = PdfTextNormalizer.normalize(pageText);
                if (!normalizedText.isEmpty()) normalizedText.append("\n\n");
                normalizedText.append(pageStartMarker(pageNo)).append("\n\n");
                if (pageText != null && !pageText.isBlank()) normalizedText.append(pageText).append("\n\n");
                normalizedText.append(pageEndMarker(pageNo));
            }
            log.debug("pdf text extraction finished. pages={} charCount={} normalizedCharCount={}",
                    pageCount, charCount, normalizedText.length());
            return normalizedText.toString().trim();
        } catch (IOException e) {
            log.error("pdf text extraction failed. source={}", pdfFile.getName(), e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_CONTENT_READ_FAILED);
        }
    }
}
