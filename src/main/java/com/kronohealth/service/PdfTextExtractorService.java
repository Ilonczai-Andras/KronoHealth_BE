package com.kronohealth.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Extracts plain text from a PDF using Apache PDFBox.
 * No external API call – runs entirely in-process.
 */
@Service
@Slf4j
public class PdfTextExtractorService {

    @Value("${app.analysis.pdf.max-chars:15000}")
    private int maxChars;

    /**
     * Reads the given InputStream, extracts all text from the PDF,
     * and truncates to {@code maxChars} if needed (to stay within AI context limits).
     *
     * @param pdfStream PDF byte stream (caller is responsible for closing it)
     * @return extracted plain text
     * @throws RuntimeException if the PDF cannot be parsed
     */
    public String extractText(InputStream pdfStream) {
        try {
            byte[] bytes = pdfStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text == null || text.isBlank()) {
                    throw new RuntimeException("A PDF nem tartalmaz kinyerhető szöveget (pl. csak képek)");
                }

                if (text.length() > maxChars) {
                    log.warn("PDF text truncated from {} to {} chars for AI analysis", text.length(), maxChars);
                    text = text.substring(0, maxChars) + "\n...[szöveg csonkítva a kontextus limit miatt]";
                }

                log.debug("Extracted {} chars from PDF", text.length());
                return text;
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("PDF text extraction failed: {}", e.getMessage());
            throw new RuntimeException("PDF szöveg kinyerés sikertelen: " + e.getMessage(), e);
        }
    }
}

