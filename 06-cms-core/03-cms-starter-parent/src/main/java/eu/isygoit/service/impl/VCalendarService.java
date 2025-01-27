package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.service.impl.CodifiableService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.exception.CalendarAlreadyExistsException;
import eu.isygoit.exception.EmptyPathException;
import eu.isygoit.exception.ResourceNotFoundException;
import eu.isygoit.jasycal.ICalendarBuilder;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.VCalendar;
import eu.isygoit.model.extendable.NextCodeModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.VCalendarRepository;
import eu.isygoit.service.IVCalendarService;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The type V calendar service.
 */
@Slf4j
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@SrvRepo(value = VCalendarRepository.class)
public class VCalendarService extends CodifiableService<Long, VCalendar, VCalendarRepository>
        implements IVCalendarService {

    private final AppProperties appProperties;

    @Autowired
    private VCalendarRepository vCalendarRepository;

    /**
     * Instantiates a new V calendar service.
     *
     * @param appProperties the app properties
     */
    public VCalendarService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Optional<VCalendar> findByDomainAndName(String domain, String name) {
        return vCalendarRepository.findByDomainIgnoreCaseAndName(domain, name);
    }


    @Override
    public VCalendar updateLockedStatus(Long id, Boolean locked) {
        repository().updateLockedStatus(locked, id);
        return repository().findById(id).orElse(null);
    }

    @Override
    public Resource download(String domain, String name) throws IOException {
        Optional<VCalendar> vCalendar = vCalendarRepository.findByDomainIgnoreCaseAndName(domain, name);
        if (!vCalendar.isPresent()) {
            throw new RuntimeException("Could not read the file!");
        } else {
            if (StringUtils.hasText(vCalendar.get().getIcsPath())) {
                Resource resource = new UrlResource(Path.of(vCalendar.get().getIcsPath()).toUri());
                if (!resource.exists()) {
                    throw new ResourceNotFoundException("for file " + domain + "/" + name);
                }
                return resource;
            } else {
                throw new EmptyPathException("for file " + domain + "/" + name);
            }
        }
    }

    @Override
    public VCalendar beforeCreate(VCalendar vCalendar) {
        this.findByDomainAndName(vCalendar.getDomain(), vCalendar.getName())
                .ifPresentOrElse(vCalendar1 -> {
                            //preparing ics file path
                            Path filePath = Path.of(appProperties.getCalanedarRepo()
                                    + File.separator + vCalendar.getDomain());
                            if (!Files.exists(filePath)) {
                                try {
                                    Files.createDirectories(filePath);
                                } catch (IOException e) {
                                    log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
                                }
                            }
                            vCalendar.setIcsPath(filePath.resolve(vCalendar.getName() + ".ics").toString());
                        },
                        () -> {
                            new CalendarAlreadyExistsException("with domain/name : " + vCalendar.getDomain() + "/" + vCalendar.getName());
                        });

        return super.beforeCreate(vCalendar);
    }

    @Override
    public VCalendar afterCreate(VCalendar vCalendar) {
        try {
            ICalendarBuilder.builder()
                    .uid(new Uid(vCalendar.getId().toString()))
                    .icsPath(vCalendar.getIcsPath())
                    .prodId(new ProdId(vCalendar.getName()))
                    .name(new Name(vCalendar.getName()))
                    .comment(new Comment(vCalendar.getName()))
                    .description(new Description(vCalendar.getName()))
                    .calScale(ImmutableCalScale.GREGORIAN)
                    .version(ImmutableVersion.VERSION_2_0)
                    .build()
                    .calendar()
                    .store();
        } catch (IOException e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
        }

        return super.afterCreate(vCalendar);
    }

    @Override
    public Optional<NextCodeModel> initCodeGenerator() {
        return Optional.ofNullable(AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(VCalendar.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("CAL")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build());
    }


}
