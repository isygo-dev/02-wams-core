package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.exception.FileAlreadyExistsException;
import eu.isygoit.exception.FileNotFoundException;
import eu.isygoit.exception.LinkedFileNotFoundException;
import eu.isygoit.exception.ResourceNotFoundException;
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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;

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

    @Autowired
    private LinkedFileRepository linkedFileRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SmsStorageLinkedFileService smsStorageLinkedFileService;

    /**
     * Instantiates a new Linked file service.
     *
     * @param appProperties the app properties
     */
    public LinkedFileService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String upload(LinkedFileRequestDto dto, MultipartFile file) throws IOException {
        // Read bytes once and calculate checksums
        byte[] buffer = file.getInputStream().readAllBytes();
        long crc16 = CRC16Helper.calculate(buffer);
        long crc32 = CRC32Helper.calculate(buffer);
        long fileSize = buffer.length;

        // Fetch existing files
        var existingFiles = linkedFileRepository
                .findByDomainIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(dto.getDomain(), dto.getOriginalFileName());

        // Deduplication check
        if (appProperties.getDoNotDuplicate() && !existingFiles.isEmpty()) {
            var lastFile = existingFiles.get(existingFiles.size() - 1); // Java 21 preferred; or get(size - 1)
            if (crc32 == lastFile.getCrc32() && crc16 == lastFile.getCrc16()) {
                throw new FileAlreadyExistsException(file.getOriginalFilename());
            }
        }

        // Handle categories (avoid repeated DB saves if category already exists)
        List<Category> categories = dto.getCategoryNames() == null ? List.of() :
                dto.getCategoryNames().stream()
                        .map(name -> categoryRepository.findByName(name)
                                .orElseGet(() -> categoryRepository.save(
                                        Category.builder().name(name).description(name).build())))
                        .toList();

        // Set default code if missing
        if (!StringUtils.hasText(dto.getCode())) {
            dto.setCode(this.getNextCode());
        }

        // Storage
        log.info("{} storage enabled - domain: {}, file: {}, tags: {}",
                appProperties.getLocalStorageActive() ? "Local" : "Remote",
                dto.getDomain(), file.getOriginalFilename(), dto.getTags());

        if (appProperties.getLocalStorageActive()) {
            storeInLocalFileSystem(dto, file);
        } else {
            storeInRemoteHostedStorageSystem(dto, file);
        }

        // Build file entity
        LinkedFile linkedFile = LinkedFile.builder()
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
                .version(existingFiles.isEmpty() ? 1L : existingFiles.size() + 1L)
                .build();

        // Save (update if code already exists)
        linkedFileRepository.findByCodeIgnoreCase(dto.getCode())
                .ifPresent(existing -> linkedFile.setId(existing.getId()));

        linkedFileRepository.save(linkedFile);

        return dto.getCode();
    }

    private void storeInRemoteHostedStorageSystem(LinkedFileRequestDto linkedFileRequestDto, MultipartFile file) {
        try {
            ResponseEntity<Object> result = smsStorageLinkedFileService.upload(
                    RequestContextDto.builder().build(),
                    linkedFileRequestDto.getDomain(),
                    linkedFileRequestDto.getDomain(),
                    linkedFileRequestDto.getPath().replace(File.separator, "#"),
                    file.getOriginalFilename(),
                    linkedFileRequestDto.getTags(),
                    file
            );
            if (result.getStatusCode().is2xxSuccessful()) {
                log.info("File uploaded successfully domain:{}, file:{} , tags:{}",
                        linkedFileRequestDto.getDomain(),
                        file.getOriginalFilename(), linkedFileRequestDto.getTags());
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }
    }

    @Override
    public List<LinkedFile> searchByTags(String domain, String tags) throws IOException {
        return linkedFileRepository.findByDomainIgnoreCaseAndTagsContaining(domain, tags);
    }

    @Override
    public void deleteFile(String domain, String code) throws IOException {
        Optional<LinkedFile> optional = linkedFileRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(domain, code);
        if (optional.isPresent()) {
            this.delete(domain, optional.get().getId());
        } else {
            log.error("Rename File: not found with domain {} and code {}", domain, code);
            throw new FileNotFoundException("File not found with domain:" + domain + " code:" + code);
        }
    }

    @Override
    public LinkedFile searchByOriginalFileName(String domain, String originalFileName) throws IOException {
        List<LinkedFile> sameDomainAndOriginalFileNameFiles = linkedFileRepository.findByDomainIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(domain, originalFileName);
        if (!CollectionUtils.isEmpty(sameDomainAndOriginalFileNameFiles)) {
            LinkedFile linkedFile = sameDomainAndOriginalFileNameFiles.get(sameDomainAndOriginalFileNameFiles.size() - 1);
            return linkedFile;
        } else {
            log.error("Rename File: not found with domain {} and originalFileName {}", domain, originalFileName);
            throw new FileNotFoundException("File not found with domain:" + domain + " originalFileName:" + originalFileName);
        }
    }

    @Override
    public LinkedFile renameFile(String domain, String code, String newName) throws IOException {
        Optional<LinkedFile> optional = linkedFileRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(domain, code);
        if (optional.isEmpty()) {
            optional.get().setOriginalFileName(newName);
            return linkedFileRepository.save(optional.get());
        } else {
            log.error("Rename File: not found with domain {} and code {}", domain, code);
            throw new FileNotFoundException("with domain:" + domain + "/code:" + code);
        }
    }

    @Override
    public List<LinkedFile> searchByCategories(String domain, List<String> categories) throws IOException {
        return linkedFileRepository.findByDomainIgnoreCaseAndCategoriesIn(domain, categories);
    }

    @Override
    public Resource download(String domain, String code) throws IOException {
        Optional<LinkedFile> linkedFile = linkedFileRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(domain, code);
        if (!linkedFile.isPresent()) {
            throw new LinkedFileNotFoundException("with domain: " + domain + "/code:" + code);
        } else {
            log.info(linkedFile.get().getPath());
            if (appProperties.getLocalStorageActive()) {
                if (StringUtils.hasText(linkedFile.get().getPath())) {
                    Resource resource = new UrlResource(Path.of(appProperties.getUploadDirectory())
                            .resolve(domain)
                            .resolve(linkedFile.get().getPath())
                            .resolve(linkedFile.get().getCode() + '.' + linkedFile.get().getExtension()).toUri());
                    if (!resource.exists()) {
                        throw new ResourceNotFoundException("with domain:" + domain + "/code:" + code);
                    }
                    return resource;
                } else {
                    Resource resource = new UrlResource(Path.of(appProperties.getUploadDirectory())
                            .resolve(domain)
                            .resolve(linkedFile.get().getCode() + '.' + linkedFile.get().getExtension()).toUri());
                    if (!resource.exists()) {
                        throw new ResourceNotFoundException("with domain:" + domain + "/code:" + code);
                    }
                    return resource;
                }
            } else {
                log.info("Storage in minio enabled domain:{}, file:{} - {}", domain, code, linkedFile.get().getOriginalFileName());
                ResponseEntity<Resource> result = smsStorageLinkedFileService.download(
                        RequestContextDto.builder().build(),
                        linkedFile.get().getDomain(),
                        linkedFile.get().getDomain(),
                        linkedFile.get().getPath().replace(File.separator, "#"),
                        linkedFile.get().getOriginalFileName(),
                        "");
                if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                    return result.getBody();
                } else {
                    return null;
                }
            }
        }
    }

    private void storeInLocalFileSystem(LinkedFileRequestDto linkedFile, MultipartFile file) throws IOException {
        Path target = Path.of(appProperties.getUploadDirectory())
                .resolve(linkedFile.getDomain())
                .resolve(linkedFile.getPath());
        FileHelper.saveMultipartFile(target,
                linkedFile.getCode(),
                file,
                FilenameUtils.getExtension(file.getOriginalFilename()),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.SYNC);
    }

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
