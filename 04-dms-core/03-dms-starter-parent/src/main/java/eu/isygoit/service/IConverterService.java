package eu.isygoit.service;

import java.io.File;
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
     */
    File doConvertPdfToHtml(final InputStream inputFile);

    /**
     * Do convert html to pdf file.
     *
     * @param inputFile the input file
     * @return the file
     */
    File doConvertHtmlToPdf(final InputStream inputFile);

    /**
     * Do convert pdf to text file.
     *
     * @param inputStream the input stream
     * @return the file
     */
    File doConvertPdfToText(InputStream inputStream);
}
