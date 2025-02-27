package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * The type Linked file service.
 */
@Slf4j
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@SrvRepo(value = LinkedFileRepository.class)
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
    public String upload(LinkedFileRequestDto linkedFileRequestDto, MultipartFile file) throws IOException {
        //Calculate the 16 and 32 bit checksum
        byte[] buffer = file.getInputStream().readAllBytes();
        long crc16 = CRC16Helper.calculate(buffer);
        long crc32 = CRC32Helper.calculate(buffer);

        //fetch same domain and original file name files
        List<LinkedFile> sameDomainAndOriginalFileNameFiles = linkedFileRepository.findByDomainIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(linkedFileRequestDto.getDomain(),
                linkedFileRequestDto.getOriginalFileName());

        if (appProperties.getDoNotDuplicate()) {
            //Verify if the file is already uploaded for the same domain
            if (!CollectionUtils.isEmpty(sameDomainAndOriginalFileNameFiles)) {
                LinkedFile linkedFile = sameDomainAndOriginalFileNameFiles.get(sameDomainAndOriginalFileNameFiles.size() - 1);
                if (crc32 == linkedFile.getCrc32() && crc16 == linkedFile.getCrc16()) {
                    throw new FileAlreadyExistsException(file.getOriginalFilename());
                }
            }
        }

        //Collect and create categories
        final List<Category> categories = new ArrayList<>();
        if (!CollectionUtils.isEmpty(linkedFileRequestDto.getCategoryNames())) {
            linkedFileRequestDto.getCategoryNames().forEach(category -> {
                Optional<Category> optional = categoryRepository.findByName(category);
                if (!optional.isPresent()) {
                    categories.add(categoryRepository.save(Category.builder()
                            .name(category)
                            .description(category)
                            .build()));
                } else {
                    categories.add(optional.get());
                }
            });
        }

        if (!StringUtils.hasText(linkedFileRequestDto.getCode())) {
            linkedFileRequestDto.setCode(this.getNextCode());
        }

        if (appProperties.getLocalStorageActive()) {
            log.info("Local file system enabled domain:{}, file:{} , tags:{}",
                    linkedFileRequestDto.getDomain(),
                    file.getOriginalFilename(),
                    linkedFileRequestDto.getTags());
            //Use local storage file system
            storeInLocalFileSystem(linkedFileRequestDto, file);
        } else {
            //Use remote hosted storage system
            log.info("Remote hosted storage system enabled domain:{}, file:{} , tags:{}",
                    linkedFileRequestDto.getDomain(),
                    file.getOriginalFilename(),
                    linkedFileRequestDto.getTags());
            storeInRemoteHostedStorageSystem(linkedFileRequestDto, file);
        }

        //Persist linked file instance
        linkedFileRepository.save(LinkedFile.builder()
                .code(linkedFileRequestDto.getCode())
                .originalFileName(file.getOriginalFilename())
                .extension(FilenameUtils.getExtension(file.getOriginalFilename()))
                .domain(linkedFileRequestDto.getDomain())
                .tags(linkedFileRequestDto.getTags())
                .crc16(crc16)
                .crc32(crc32)
                .size(file.getSize())
                .path(linkedFileRequestDto.getPath())
                .categories(categories)
                .mimetype(file.getContentType())
                .version(CollectionUtils.isEmpty(sameDomainAndOriginalFileNameFiles) ? 1L : (long) (sameDomainAndOriginalFileNameFiles.size() + 1))
                .build());

        return linkedFileRequestDto.getCode();
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
                    Resource resource = new UrlResource(Path.of(appProperties.getUploadDirectory() +
                            File.separator + domain +
                            File.separator + linkedFile.get().getPath() +
                            File.separator + linkedFile.get().getCode() + '.' + linkedFile.get().getExtension()).toUri());
                    if (!resource.exists()) {
                        throw new ResourceNotFoundException("with domain:" + domain + "/code:" + code);
                    }
                    return resource;
                } else {
                    Resource resource = new UrlResource(Path.of(appProperties.getUploadDirectory()
                            + File.separator + domain
                            + File.separator + linkedFile.get().getCode() + '.' + linkedFile.get().getExtension()).toUri());
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
                FilenameUtils.getExtension(file.getOriginalFilename()));
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
