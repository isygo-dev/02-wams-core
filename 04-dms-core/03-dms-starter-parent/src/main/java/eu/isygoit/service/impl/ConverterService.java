package eu.isygoit.service.impl;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import eu.isygoit.config.AppProperties;
import eu.isygoit.service.IConverterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.fit.pdfdom.PDFDomTree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * ConverterService provides implementations for converting files
 * between PDF, HTML, and plain text formats.
 */
@Slf4j
@Service
@Transactional
public class ConverterService implements IConverterService {

    private final AppProperties appProperties;

    public ConverterService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Generates a temporary file path and executes a function that uses it.
     *
     * @param extension File extension (e.g., .pdf, .html, .txt)
     * @param action    The function to execute with the path
     * @param <T>       Return type
     * @return The result from the function
     * @throws IOException In case of any IO error
     */
    private <T> T withGeneratedTempPath(String extension, WithTempPath<T> action) throws IOException {
        Path tempPath = Path.of(appProperties.getUploadDirectory(), "convert", "temp", UUID.randomUUID() + extension);
        Files.createDirectories(tempPath.getParent());
        try {
            return action.apply(tempPath);
        } catch (Exception e) {
            log.error("Error while handling temporary path operation: {}", tempPath, e);
            throw (e instanceof IOException io) ? io : new IOException("Unexpected error", e);
        }
    }

    /**
     * Converts a PDF input stream to an HTML file.
     *
     * @param inputFile PDF file input stream
     * @return Generated HTML file
     * @throws IOException if the conversion fails
     */
    @Override
    public File doConvertPdfToHtml(final InputStream inputFile) throws IOException {
        log.info("Converting PDF to HTML...");
        return withGeneratedTempPath(".html", htmlPath -> {
            try (PDDocument pdDocument = PDDocument.load(inputFile);
                 Writer writer = Files.newBufferedWriter(htmlPath, StandardCharsets.UTF_8)) {
                new PDFDomTree().writeText(pdDocument, writer);
                log.info("PDF successfully converted to HTML at {}", htmlPath);
                return htmlPath.toFile();
            }
        });
    }

    /**
     * Converts an HTML input stream to a PDF file.
     *
     * @param inputFile HTML input stream
     * @return Generated PDF file
     * @throws DocumentException If PDF creation fails
     * @throws IOException       If file IO fails
     */
    @Override
    public File doConvertHtmlToPdf(final InputStream inputFile) throws DocumentException, IOException {
        log.info("Converting HTML to PDF...");
        try {
            return withGeneratedTempPath(".pdf", pdfPath -> {
                try (OutputStream os = Files.newOutputStream(pdfPath)) {
                    Document document = new Document();
                    PdfWriter writer = PdfWriter.getInstance(document, os);
                    document.open();
                    XMLWorkerHelper.getInstance().parseXHtml(writer, document, inputFile, StandardCharsets.UTF_8);
                    document.close();
                    log.info("HTML successfully converted to PDF at {}", pdfPath);
                    return pdfPath.toFile();
                } catch (DocumentException | IOException e) {
                    log.error("Error converting HTML to PDF", e);
                    throw e;
                }
            });
        } catch (IOException e) {
            throw new IOException("Failed to convert HTML to PDF", e);
        }
    }

    /**
     * Converts a PDF input stream to a plain text file.
     *
     * @param inputStream PDF file input stream
     * @return Generated TXT file
     * @throws IOException if the conversion fails
     */
    @Override
    public File doConvertPdfToText(InputStream inputStream) throws IOException {
        log.info("Converting PDF to Text...");

        // Save PDF to temp file first
        return withGeneratedTempPath(".pdf", pdfPath -> {
            FileUtils.copyInputStreamToFile(inputStream, pdfPath.toFile());

            return withGeneratedTempPath(".txt", textPath -> {
                try (PDDocument document = PDDocument.load(pdfPath.toFile());
                     Writer writer = Files.newBufferedWriter(textPath, StandardCharsets.UTF_8)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    writer.write(stripper.getText(document));
                    log.info("PDF successfully converted to Text at {}", textPath);
                    return textPath.toFile();
                } catch (IOException e) {
                    log.error("Error converting PDF to text", e);
                    throw e;
                }
            });
        });
    }

    /**
     * Functional interface used to encapsulate logic that needs a temporary file path.
     */
    @FunctionalInterface
    private interface WithTempPath<T> {
        T apply(Path path) throws Exception;
    }
}