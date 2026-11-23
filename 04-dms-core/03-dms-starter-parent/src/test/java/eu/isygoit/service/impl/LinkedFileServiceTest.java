package eu.isygoit.service.impl;

import eu.isygoit.config.AppProperties;
import eu.isygoit.exception.StoreFileException;
import eu.isygoit.model.LinkedFile;
import eu.isygoit.remote.sms.SmsStorageLinkedFileService;
import eu.isygoit.repository.CategoryRepository;
import eu.isygoit.repository.LinkedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkedFileServiceTest {

    @TempDir
    Path tempDirectory;
    @Mock
    private AppProperties appProperties;
    @Mock
    private LinkedFileRepository linkedFileRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SmsStorageLinkedFileService smsStorageLinkedFileService;
    private LinkedFileService linkedFileService;

    @BeforeEach
    void setUp() {
        linkedFileService = spy(new LinkedFileService(
                appProperties,
                linkedFileRepository,
                categoryRepository,
                smsStorageLinkedFileService));
    }

    @Test
    void deleteFileRemovesRemoteObjectBeforeDeletingDatabaseRecord() throws Exception {
        LinkedFile file = linkedFile();
        when(linkedFileRepository.findByTenantIgnoreCaseAndCodeIgnoreCase("tenant", "FLE000001"))
                .thenReturn(Optional.of(file));
        when(appProperties.getLocalStorageActive()).thenReturn(false);
        when(smsStorageLinkedFileService.delete("tenant", "tenant", "documents", "report.pdf"))
                .thenReturn(ResponseEntity.ok(new Object()));
        doNothing().when(linkedFileService).delete("tenant", 1L);

        linkedFileService.deleteFile("tenant", "FLE000001");

        InOrder order = inOrder(smsStorageLinkedFileService, linkedFileService);
        order.verify(smsStorageLinkedFileService).delete("tenant", "tenant", "documents", "report.pdf");
        order.verify(linkedFileService).delete("tenant", 1L);
    }

    @Test
    void deleteFileKeepsDatabaseRecordWhenRemoteDeletionFails() {
        LinkedFile file = linkedFile();
        when(linkedFileRepository.findByTenantIgnoreCaseAndCodeIgnoreCase("tenant", "FLE000001"))
                .thenReturn(Optional.of(file));
        when(appProperties.getLocalStorageActive()).thenReturn(false);
        when(smsStorageLinkedFileService.delete("tenant", "tenant", "documents", "report.pdf"))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).<Object>body("unavailable"));

        assertThrows(StoreFileException.class, () -> linkedFileService.deleteFile("tenant", "FLE000001"));

        verify(linkedFileService, never()).delete("tenant", 1L);
    }

    @Test
    void deleteFileRemovesLocalObjectBeforeDeletingDatabaseRecord() throws Exception {
        LinkedFile file = linkedFile();
        Path localFile = tempDirectory.resolve("tenant").resolve("documents").resolve("FLE000001.pdf");
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "file contents");
        when(linkedFileRepository.findByTenantIgnoreCaseAndCodeIgnoreCase("tenant", "FLE000001"))
                .thenReturn(Optional.of(file));
        when(appProperties.getLocalStorageActive()).thenReturn(true);
        when(appProperties.getUploadDirectory()).thenReturn(tempDirectory.toString());
        doNothing().when(linkedFileService).delete("tenant", 1L);

        linkedFileService.deleteFile("tenant", "FLE000001");

        assertFalse(Files.exists(localFile));
        verify(linkedFileService).delete("tenant", 1L);
    }

    private LinkedFile linkedFile() {
        return LinkedFile.builder()
                .id(1L)
                .tenant("tenant")
                .code("FLE000001")
                .path("documents")
                .originalFileName("report.pdf")
                .extension("pdf")
                .build();
    }
}
