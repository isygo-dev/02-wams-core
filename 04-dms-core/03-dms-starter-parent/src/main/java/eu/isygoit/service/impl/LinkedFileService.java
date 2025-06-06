package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.exception.*;
import eu.isygoit.helper.CRC16Helper;
import eu.isygoit.helper.CRC32Helper;
import eu.isygoit.helper.FileHelper;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.Category;
import eu.isygoit.model.LinkedFile;
import eu.isygoit.model.extendable.NextCodeModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.remote.sms.SmsStorageLinkedFileService;
import eu.isygoit.repository.CategoryRepository;
import eu.isygoit.repository.LinkedFileRepository;
import eu.isygoit.service.ILinkedFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * The type Linked file service.
 */
@Slf4j
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@ServRepo(value = LinkedFileRepository.class)
public class LinkedFileService extends CodeAssignableService<Long, LinkedFile, LinkedFileRepository> implements ILinkedFileService {

    private final AppProperties appProperties;
    private final LinkedFileRepository linkedFileRepository;
    private final CategoryRepository categoryRepository;
    private final SmsStorageLinkedFileService smsStorageLinkedFileService;

    public LinkedFileService(AppProperties appProperties,
                             LinkedFileRepository linkedFileRepository,
                             CategoryRepository categoryRepository,
                             SmsStorageLinkedFileService smsStorageLinkedFileService) {
        this.appProperties = appProperties;
        this.linkedFileRepository = linkedFileRepository;
        this.categoryRepository = categoryRepository;
        this.smsStorageLinkedFileService = smsStorageLinkedFileService;
    }

    /**
     * Uploads a file to either local or remote storage, handles deduplication,
     * and saves metadata in the database.
     */
    @Override
    public String upload(LinkedFileRequestDto dto, MultipartFile file) throws IOException {
        byte[] content = file.getInputStream().readAllBytes();
        long crc16 = CRC16Helper.calculate(content);
        long crc32 = CRC32Helper.calculate(content);

        // Check for file duplication based on name and CRCs
        List<LinkedFile> existingFiles = linkedFileRepository
                .findByDomainIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(dto.getDomain(), dto.getOriginalFileName());

        if (appProperties.getDoNotDuplicate() && !existingFiles.isEmpty()) {
            var lastFile = existingFiles.get(existingFiles.size() - 1); // Java 21 preferred; or get(size - 1)
            if (crc32 == lastFile.getCrc32() && crc16 == lastFile.getCrc16()) {
                throw new FileAlreadyExistsException("Duplicate file: " + file.getOriginalFilename());
            }
        }

        // Resolve categories from names (create them if necessary)
        List<Category> categories = resolveCategories(dto.getCategoryNames());

        // Generate a code if not provided
        if (!StringUtils.hasText(dto.getCode())) {
            dto.setCode(getNextCode());
        }

        log.info("{} storage enabled - domain: {}, file: {}, tags: {}",
                appProperties.getLocalStorageActive() ? "Local" : "Remote",
                dto.getDomain(), file.getOriginalFilename(), dto.getTags());

        // Store file either locally or remotely
        if (appProperties.getLocalStorageActive()) {
            try {
                storeInLocalFileSystem(dto, file);
            } catch (IOException e) {
                log.error("File local storage failed", e);
                throw new StoreFileException("Unable to loaclly store file: " + file.getOriginalFilename(), e);
            }
        } else {
            storeInRemoteHostedStorageSystem(dto, file);
        }


        // Build LinkedFile entity
        LinkedFile linkedFile = buildLinkedFile(dto, file, content.length, crc16, crc32, categories, existingFiles.size());

        // Update existing record if same code already exists
        linkedFileRepository.findByCodeIgnoreCase(dto.getCode())
                .ifPresent(existing -> linkedFile.setId(existing.getId()));

        linkedFileRepository.save(linkedFile);
        return dto.getCode();
    }

    /**
     * Resolves a list of category names into Category entities (creates if not found).
     */
    private List<Category> resolveCategories(List<String> categoryNames) {
        if (CollectionUtils.isEmpty(categoryNames)) return List.of();
        return categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                        .orElseGet(() -> categoryRepository.save(Category.builder().name(name).description(name).build())))
                .toList();
    }

    /**
     * Creates a LinkedFile entity with calculated fields and resolved relationships.
     */
    private LinkedFile buildLinkedFile(LinkedFileRequestDto dto, MultipartFile file, long fileSize,
                                       long crc16, long crc32, List<Category> categories, int existingCount) {
        return LinkedFile.builder()
                .code(dto.getCode())
                .originalFileName(file.getOriginalFilename())
                .extension(FilenameUtils.getExtension(file.getOriginalFilename()))
                .domain(dto.getDomain())
                .tags(dto.getTags())
                .crc16(crc16)
                .crc32(crc32)
                .size(fileSize)
                .path(dto.getPath())
                .categories(categories)
                .mimetype(file.getContentType())
                .version(existingCount == 0 ? 1L : existingCount + 1L)
                .build();
    }

    /**
     * Stores the file in a remote file storage system.
     */
    private void storeInRemoteHostedStorageSystem(LinkedFileRequestDto dto, MultipartFile file) {
        try {
            ResponseEntity<Object> response = smsStorageLinkedFileService.upload(
                    RequestContextDto.builder().build(),
                    dto.getDomain(), dto.getDomain(),
                    dto.getPath().replace(File.separator, "#"),
                    file.getOriginalFilename(),
                    dto.getTags(), file
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Remote upload success: domain={}, file={}, tags={}",
                        dto.getDomain(), file.getOriginalFilename(), dto.getTags());
            }
        } catch (Exception e) {
            log.error("Remote storage error", e);
        }
    }

    /**
     * Stores the file in the local filesystem.
     */
    private void storeInLocalFileSystem(LinkedFileRequestDto dto, MultipartFile file) throws IOException {
        Path target = Path.of(appProperties.getUploadDirectory(), dto.getDomain(), dto.getPath());
        FileHelper.saveMultipartFile(target, dto.getCode(), file,
                FilenameUtils.getExtension(file.getOriginalFilename()),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
    }

    /**
     * Downloads a file (either locally or remotely) using domain and code.
     */
    @Override
    public Resource download(String domain, String code) throws IOException {
        LinkedFile file = linkedFileRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(domain, code)
                .orElseThrow(() -> new LinkedFileNotFoundException("with domain: " + domain + "/code:" + code));

        if (appProperties.getLocalStorageActive()) {
            return resolveLocalFile(domain, file);
        } else {
            ResponseEntity<Resource> response = smsStorageLinkedFileService.download(
                    RequestContextDto.builder().build(),
                    file.getDomain(), file.getDomain(),
                    file.getPath().replace(File.separator, "#"),
                    file.getOriginalFileName(), "");

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new FileNotFoundException("Remote file not found");
            }
            return response.getBody();
        }
    }

    /**
     * Resolves a local file path and loads it as a Resource.
     */
    private Resource resolveLocalFile(String domain, LinkedFile file) throws IOException {
        Path basePath = Path.of(appProperties.getUploadDirectory(), domain);
        Path filePath = StringUtils.hasText(file.getPath()) ?
                basePath.resolve(file.getPath()) : basePath;
        Path fullPath = filePath.resolve(file.getCode() + "." + file.getExtension());

        Resource resource = new UrlResource(fullPath.toUri());
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found with domain:" + domain + "/code:" + file.getCode());
        }
        return resource;
    }

    /**
     * Deletes a file using its domain and code.
     */
    @Override
    public void deleteFile(String domain, String code) throws IOException {
        LinkedFile file = linkedFileRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(domain, code)
                .orElseThrow(() -> new FileNotFoundException("File not found with domain:" + domain + " code:" + code));
        this.delete(domain, file.getId());
    }

    /**
     * Finds the most recent uploaded file with the same original file name.
     */
    @Override
    public LinkedFile searchByOriginalFileName(String domain, String originalFileName) throws IOException {
        return linkedFileRepository.findByDomainIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(domain, originalFileName)
                .stream()
                .reduce((first, second) -> second) // Get last (most recent)
                .orElseThrow(() -> new FileNotFoundException("File not found with domain:" + domain + " originalFileName:" + originalFileName));
    }

    /**
     * Updates the original filename of a stored file.
     */
    @Override
    public LinkedFile renameFile(String domain, String code, String newName) throws IOException {
        LinkedFile file = linkedFileRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(domain, code)
                .orElseThrow(() -> new FileNotFoundException("with domain:" + domain + "/code:" + code));
        file.setOriginalFileName(newName);
        return linkedFileRepository.save(file);
    }

    /**
     * Searches files by matching tags.
     */
    @Override
    public List<LinkedFile> searchByTags(String domain, String tags) {
        return linkedFileRepository.findByDomainIgnoreCaseAndTagsContaining(domain, tags);
    }

    /**
     * Searches files by matching categories.
     */
    @Override
    public List<LinkedFile> searchByCategories(String domain, List<String> categories) {
        return linkedFileRepository.findByDomainIgnoreCaseAndCategoriesIn(domain, categories);
    }

    /**
     * Initializes the code generation strategy used for file codes.
     */
    @Override
    public NextCodeModel initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(LinkedFile.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("FLE")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}