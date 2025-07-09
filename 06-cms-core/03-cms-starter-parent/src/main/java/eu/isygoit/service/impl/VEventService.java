package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.exception.CalendarNotFoundException;
import eu.isygoit.jasycal.ICalendar;
import eu.isygoit.jasycal.ICalendarBuilder;
import eu.isygoit.jasycal.IEvent;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.VCalendar;
import eu.isygoit.model.VCalendarEvent;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.VEventRepository;
import eu.isygoit.service.IVEventService;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.property.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The type V event service.
 */
@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = VEventRepository.class)
public class VEventService extends CodeAssignableTenantService<Long, VCalendarEvent, VEventRepository> implements IVEventService {

    private final AppProperties appProperties;

    @Autowired
    private VCalendarService calendarService;

    /**
     * Instantiates a new V event service.
     *
     * @param appProperties the app properties
     */
    public VEventService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public List<VCalendarEvent> findByTenantAndCalendar(String tenant, String calendar) {
        return repository().findByTenantIgnoreCaseAndCalendar(tenant, calendar);
    }

    /**
     * Find by tenant and calendar and code optional.
     *
     * @param tenant   the tenant
     * @param calendar the calendar
     * @param code     the code
     * @return the optional
     */
    public Optional<VCalendarEvent> findByTenantAndCalendarAndCode(String tenant, String calendar, String code) {
        return repository().findByTenantIgnoreCaseAndCalendarAndCodeIgnoreCase(tenant, calendar, code);
    }

    @Override
    public VCalendarEvent beforeCreate(String tenant, VCalendarEvent vCalendarEvent) {
        VCalendar vCalendar = calendarService.findByTenantAndName(vCalendarEvent.getTenant(), vCalendarEvent.getCalendar());
        if (vCalendar == null) {
            if (appProperties.getCreateCalendarIfNotExists()) {
                calendarService.create(vCalendarEvent.getTenant(),
                        VCalendar.builder()
                        .tenant(vCalendarEvent.getTenant())
                        .name(vCalendarEvent.getCalendar())
                        .description(vCalendarEvent.getCalendar())
                        .build());
            } else {
                throw new CalendarNotFoundException("with tenant/name : " + vCalendarEvent.getTenant() + "/" + vCalendarEvent.getCalendar());
            }
        }
        return super.beforeCreate(tenant, vCalendarEvent);
    }

    @Override
    public VCalendarEvent afterCreate(String tenant, VCalendarEvent vCalendarEvent) {
        VCalendar vCalendar = calendarService.findByTenantAndName(vCalendarEvent.getTenant(), vCalendarEvent.getCalendar());
        if (vCalendar == null) {
            throw new CalendarNotFoundException("with tenant/name : " + vCalendarEvent.getTenant() + "/" + vCalendarEvent.getCalendar());
        }

        try {
            ICalendar calendar = ICalendarBuilder.builder()
                    .icsPath(vCalendar.getIcsPath())
                    .build()
                    .calendar()
                    .load()
                    .addEvent(IEvent.builder()
                            .uid(new Uid(vCalendarEvent.getId().toString()))
                            .name(new Name(vCalendarEvent.getName()))
                            .description(new Description(vCalendarEvent.getName()))
                            .summary(new Summary(vCalendarEvent.getName()))
                            .dtStart(new DtStart(vCalendarEvent.getStartDate().toInstant()))
                            .dtEnd(new DtEnd(vCalendarEvent.getEndDate().toInstant()))
                            .build()
                            .event())
                    .store();
        } catch (IOException e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
        } catch (ParserException e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
        }
        return super.afterCreate(tenant, vCalendarEvent);
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(VCalendarEvent.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("EVT")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}
