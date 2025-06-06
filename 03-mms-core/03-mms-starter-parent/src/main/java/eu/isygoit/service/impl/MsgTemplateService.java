package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.FileService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.helper.FileHelper;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.MsgTemplate;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.ims.ImsPublicService;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.MsgTemplateRepository;
import eu.isygoit.service.IMsgTemplateService;
import eu.isygoit.types.MsgTemplateVariables;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * The type Template service.
 */
@Slf4j
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@ServRepo(value = MsgTemplateRepository.class)
public class MsgTemplateService extends FileService<Long, MsgTemplate, MsgTemplateRepository>
        implements IMsgTemplateService {

    private final AppProperties appProperties;

    @Autowired
    private MsgTemplateRepository templateRepository;

    @Autowired
    private ImsPublicService imsPublicService;

    @Autowired
    private FreeMarkerConfigurer freemarkerConfig;

    /**
     * Instantiates a new Template service.
     *
     * @param appProperties the app properties
     */
    public MsgTemplateService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void beforeDelete(Long id) {
        Optional<MsgTemplate> optional = templateRepository.findById(id);
        if (optional.isPresent()) {
            deleteFile(Path.of(optional.get().getPath())
                    .resolve(optional.get().getOriginalFileName()).toString());
        }
        super.beforeDelete(id);
    }

    private void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    @Override
    public String composeMessageBody(String senderDomainName,
                                     IEnumEmailTemplate.Types templateName,
                                     Map<String, String> variables)
            throws IOException, TemplateException {
        Optional<MsgTemplate> optional = templateRepository.findByDomainIgnoreCaseAndName(senderDomainName, templateName);
        MsgTemplate template = null;
        if (optional.isPresent()) {
            template = optional.get();
        } else {
            log.warn("Template {} is not present for domain {}, we will create a default one!", templateName, senderDomainName);
            Path filePath = Path.of(this.getUploadDirectory())
                    .resolve(senderDomainName)
                    .resolve(MsgTemplate.class.getSimpleName().toLowerCase());
            template = this.create(MsgTemplate.builder()
                    .domain(senderDomainName)
                    .name(templateName)
                    .description(templateName.meaning())
                    .path(filePath.toString())
                    .language(IEnumLanguage.Types.EN)
                    .build());
            log.info(Path.of(MsgTemplate.class.getSimpleName().toLowerCase(), templateName.name().toLowerCase() + ".ftl").toUri().getPath());
            Resource resource = new UrlResource(Path.of(MsgTemplate.class.getSimpleName().toLowerCase(), templateName.name().toLowerCase() + ".ftl").toUri());
            if (resource.exists()) {
                log.info("Copying Template resource: {}", Path.of(MsgTemplate.class.getSimpleName().toLowerCase())
                        .resolve(templateName.name().toLowerCase() + ".ftl").toString());
                template.setOriginalFileName(templateName.name().toLowerCase() + ".ftl");
                template.setFileName(template.getCode().toLowerCase() + "." + FilenameUtils.getExtension(resource.getFilename()));
                template.setExtension(FilenameUtils.getExtension(resource.getFilename()));
                this.update(template);
                FileHelper.createDirectoryIfAbsent(filePath);
                FileUtils.copyURLToFile(resource.getURL(), new File(filePath.toString(), template.getFileName()));
            } else {
                log.error("<Error>: Template resource not found: {}", Path.of(MsgTemplate.class.getSimpleName().toLowerCase())
                        .resolve(templateName.name().toLowerCase() + ".ftl").toString());
            }
        }

        FileTemplateLoader templateLoader = new FileTemplateLoader(new File(template.getPath()));
        freemarkerConfig.getConfiguration().setTemplateLoader(templateLoader);

        try {
            ResponseEntity<DomainDto> result = imsPublicService.getDomainByName(senderDomainName);
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody()) {
                DomainDto domain = result.getBody();
                variables.put(MsgTemplateVariables.V_DOMAIN_URL, domain.getUrl() != null ? domain.getUrl() : "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_PHONE, domain.getPhone() != null ? domain.getPhone() : "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_EMAIL, domain.getEmail() != null ? domain.getEmail() : "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_ADDRESS, domain.getAddress() != null ? domain.getAddress().format() : "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_FACEBOOK, domain.getLnk_facebook() != null ? domain.getLnk_facebook() : "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_LINKEDIN, domain.getLnk_linkedin() != null ? domain.getLnk_linkedin() : "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_XING, domain.getLnk_xing() != null ? domain.getLnk_xing() : "Missed");
            } else {
                variables.put(MsgTemplateVariables.V_DOMAIN_URL, "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_PHONE, "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_EMAIL, "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_ADDRESS, "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_FACEBOOK, "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_LINKEDIN, "Missed");
                variables.put(MsgTemplateVariables.V_DOMAIN_XING, "Missed");
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }
        //get sender domain variables

        return FreeMarkerTemplateUtils.processTemplateIntoString(
                freemarkerConfig.getConfiguration().getTemplate(template.getFileName()),
                variables);
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(MsgTemplate.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("MTP")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }

    @Override
    protected String getUploadDirectory() {
        return appProperties.getUploadDirectory();
    }
}


