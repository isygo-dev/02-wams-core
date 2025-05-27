package eu.isygoit.service.impl;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import eu.isygoit.config.AppProperties;
import eu.isygoit.service.IConverterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.fit.pdfdom.PDFDomTree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

/**
 * The type Converter service.
 */
/*
 * Implement all available conversions in the link
 * https://www.baeldung.com/pdf-conversions-java
 */
@Slf4j
@Service
@Transactional
public class ConverterService implements IConverterService {

    private final AppProperties appProperties;

    /**
     * Instantiates a new Converter service.
     *
     * @param appProperties the app properties
     */
    public ConverterService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public File doConvertPdfToHtml(final InputStream inputFile) throws IOException {
        log.info("Converting from pdf to html ...");
        Path filePath = Path.of(appProperties.getUploadDirectory())
                .resolve("convert")
                .resolve("temp")
                .resolve(UUID.randomUUID() + ".html");
        PDDocument pdDocument = PDDocument.load(inputFile);
        Writer outputStream = new PrintWriter(filePath.toString(), StandardCharsets.UTF_8);
        new PDFDomTree().writeText(pdDocument, outputStream);
        outputStream.close();
        log.info("File was converted from pdf to html successfully. {}", filePath);
        return new File(filePath.toString());
    }

    @Override
    public File doConvertHtmlToPdf(final InputStream inputFile) throws DocumentException, IOException {
        log.info("Converting from html to pdf ...");
        String filePath = new StringBuilder(appProperties.getUploadDirectory())
                .append(File.separator)
                .append("convert")
                .append(File.separator)
                .append("temp")
                .append(File.separator)
                .append(UUID.randomUUID())
                .append(".pdf")
                .toString();
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document,
                new FileOutputStream(filePath));
        document.open();
        XMLWorkerHelper.getInstance().parseXHtml(writer, document, inputFile);
        document.close();
        log.info("File was converted from html to pdf successfully. {}", filePath);
        return new File(filePath);
    }

    @Override
    public File doConvertPdfToText(InputStream inputStream) throws IOException {
        //Save PDF file
        File pdfFile = new File(new StringBuilder(appProperties.getUploadDirectory())
                .append(File.separator)
                .append("convert")
                .append(File.separator)
                .append("temp")
                .append(File.separator)
                .append(UUID.randomUUID())
                .append(".pdf")
                .toString());
        FileUtils.copyInputStreamToFile(inputStream, pdfFile);
        //loading PDF
        PDFParser parser = new PDFParser(new RandomAccessFile(pdfFile, "r"));
        parser.parse();

        //Extracting Text
        COSDocument cosDoc = parser.getDocument();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDDocument pdDoc = new PDDocument(cosDoc);
        String parsedText = pdfStripper.getText(pdDoc);

        //Save Text file
        String filePath = new StringBuilder(appProperties.getUploadDirectory())
                .append(File.separator)
                .append("convert")
                .append(File.separator)
                .append("temp")
                .append(File.separator)
                .append(UUID.randomUUID())
                .append(".txt")
                .toString();
        PrintWriter pw = new PrintWriter(filePath);
        pw.print(parsedText);
        pw.close();

        return new File(filePath);
    }
}
