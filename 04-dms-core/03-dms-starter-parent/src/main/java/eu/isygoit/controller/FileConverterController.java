package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.FileConverterApi;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.enums.IEnumFile;
import eu.isygoit.exception.ConvertFileException;
import eu.isygoit.exception.ResourceNotFoundException;
import eu.isygoit.exception.UnsupportedFileTypeException;
import eu.isygoit.exception.handler.DmsExceptionHandler;
import eu.isygoit.service.IConverterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;

/**
 * The type File converter controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(DmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/file/convert")
public class FileConverterController extends ControllerExceptionHandler implements FileConverterApi {

    @Autowired
    private IConverterService converterService;

    /**
     * Maps a target file type to its MIME type.
     * <p>
     * Prefer this over {@code Files.probeContentType(...)}: it never returns {@code null}
     * and does not depend on the OS file-type detection.
     */
    private static String mimeOf(IEnumFile.Types type) {
        return switch (type) {
            case TEXT -> MediaType.TEXT_PLAIN_VALUE;
            case HTML -> MediaType.TEXT_HTML_VALUE;
            case PDF -> MediaType.APPLICATION_PDF_VALUE;
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    @Override
    public ResponseEntity<Resource> convertPdf(
            IEnumFile.Types targetFormat,
            MultipartFile file) {
        try {
            File responseFile = switch (targetFormat) {
                case TEXT -> converterService.doConvertPdfToText(file.getInputStream());
                case HTML -> converterService.doConvertPdfToHtml(file.getInputStream());
                default -> throw new UnsupportedFileTypeException(targetFormat.meaning());
            };
            return toResponse(responseFile, targetFormat);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Resource> convertHtml(
            IEnumFile.Types targetFormat,
            MultipartFile file) {
        try {
            File responseFile = switch (targetFormat) {
                case PDF -> converterService.doConvertHtmlToPdf(file.getInputStream());
                default -> throw new UnsupportedFileTypeException(targetFormat.meaning());
            };
            return toResponse(responseFile, targetFormat);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Wraps a locally produced {@link File} into a downloadable {@link ResponseEntity}.
     * <p>
     * The file is created in-process by the converter service, so it is a real file on disk
     * (unlike resources returned by Feign calls, which are in-memory byte arrays and must
     * never be probed on the filesystem).
     * <p>
     * The {@code Content-Type} is derived from the requested {@code targetFormat} rather than
     * from {@code Files.probeContentType(...)}, which can return {@code null} and is
     * OS-dependent. Using the enum guarantees a correct, portable MIME type.
     *
     * @param responseFile the converted file
     * @param targetFormat the requested target format
     * @return a {@code 200 OK} response streaming the file, or an exception response
     */
    private ResponseEntity<Resource> toResponse(File responseFile, IEnumFile.Types targetFormat) {
        if (responseFile == null) {
            throw new ConvertFileException(targetFormat.meaning());
        }
        try {
            Resource resource = new UrlResource(responseFile.toURI());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("for path: " + responseFile.toURI());
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mimeOf(targetFormat))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + responseFile.getName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ConvertFileException(targetFormat.meaning(), e);
        }
    }
}