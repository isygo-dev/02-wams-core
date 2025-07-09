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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

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

    @Override
    public ResponseEntity<Resource> convertPdf(//RequestContextDto requestContext,
                                               IEnumFile.Types targetFormat,
                                               MultipartFile file) {
        try {
            File responseFile = null;
            switch (targetFormat) {
                case TEXT -> {
                    responseFile = converterService.doConvertPdfToText(file.getInputStream());
                }
                case HTML -> {
                    responseFile = converterService.doConvertPdfToHtml(file.getInputStream());
                }
                default -> throw new UnsupportedFileTypeException(targetFormat.meaning());
            }

            if (responseFile != null) {
                Resource resource = new UrlResource(responseFile.toURI());
                if (resource.exists()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(responseFile.toPath()))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + responseFile.getName() + "\"")
                            .body(resource);
                } else {
                    throw new ResourceNotFoundException("for path: " + responseFile.toURI());
                }
            } else {
                throw new ConvertFileException(targetFormat.meaning());
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Resource> convertHtml(//RequestContextDto requestContext,
                                                IEnumFile.Types targetFormat,
                                                MultipartFile file) {
        try {
            File responseFile = null;
            switch (targetFormat) {
                case PDF -> {
                    responseFile = converterService.doConvertHtmlToPdf(file.getInputStream());
                }
                default -> throw new UnsupportedFileTypeException(targetFormat.meaning());
            }

            if (responseFile != null) {
                Resource resource = new UrlResource(responseFile.toURI());
                if (resource.exists()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(responseFile.toPath()))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + responseFile.getName() + "\"")
                            .body(resource);
                } else {
                    throw new ResourceNotFoundException("for path: " + responseFile.toURI());
                }
            } else {
                throw new ConvertFileException(targetFormat.meaning());
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
