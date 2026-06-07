package com.oriole.wisepen.document.service.impl;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.document.exception.DocumentError;
import com.oriole.wisepen.document.service.IDocumentFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFileServiceImpl implements IDocumentFileService {

    private final DocumentConverter documentConverter;

    @Override
    public void convertToPdf(File source, File target) {
        long start = System.currentTimeMillis();
        log.info("office to pdf conversion started. source={} size={}", source.getName(), source.length());
        try {
            documentConverter.convert(source).to(target).execute();
            log.info("office to pdf conversion finished. costMs={}", System.currentTimeMillis() - start);
        } catch (OfficeException e) {
            log.error("office to pdf conversion failed. costMs={} source={}", System.currentTimeMillis() - start, source.getName(), e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_CONVERT_FAILED);
        }
    }

    @Override
    public String extractText(File pdfFile) {
        log.debug("pdf text extraction started. source={}", pdfFile.getName());
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 保留段落换行，去除多余空白
            String text = stripper.getText(doc);
            log.debug("pdf text extraction finished. charCount={}", text.length());
            return text;
        } catch (IOException e) {
            log.error("pdf text extraction failed. source={}", pdfFile.getName(), e);
            throw new ServiceException(DocumentError.DOCUMENT_PROCESS_CONTENT_READ_FAILED);
        }
    }
}
