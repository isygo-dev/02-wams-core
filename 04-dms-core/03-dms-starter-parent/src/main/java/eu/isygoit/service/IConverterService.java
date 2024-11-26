package eu.isygoit.service;

import com.itextpdf.text.DocumentException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * The interface Converter service.
 */
public interface IConverterService {

    /**
     * Do convert pdf to html file.
     *
     * @param inputFile the input file
     * @return the file
     * @throws IOException the io exception
     */
    File doConvertPdfToHtml(final InputStream inputFile) throws IOException;

    /**
     * Do convert html to pdf file.
     *
     * @param inputFile the input file
     * @return the file
     * @throws DocumentException the document exception
     * @throws IOException       the io exception
     */
    File doConvertHtmlToPdf(final InputStream inputFile) throws DocumentException, IOException;

    /**
     * Do convert pdf to text file.
     *
     * @param inputStream the input stream
     * @return the file
     * @throws IOException the io exception
     */
    File doConvertPdfToText(InputStream inputStream) throws IOException;
}
