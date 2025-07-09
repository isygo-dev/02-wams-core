package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.CalendarEventControllerAPI;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.VCalendarEventDto;
import eu.isygoit.exception.CalendarNotFoundException;
import eu.isygoit.exception.handler.CmsExceptionHandler;
import eu.isygoit.jasycal.ICalendar;
import eu.isygoit.jasycal.ICalendarBuilder;
import eu.isygoit.jasycal.IEvent;
import eu.isygoit.mapper.VCalendarEventMapper;
import eu.isygoit.model.VCalendar;
import eu.isygoit.model.VCalendarEvent;
import eu.isygoit.service.impl.VCalendarService;
import eu.isygoit.service.impl.VEventService;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.property.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The type V event controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = CmsExceptionHandler.class, mapper = VCalendarEventMapper.class, minMapper = VCalendarEventMapper.class, service = VEventService.class)
@RequestMapping(path = "/api/v1/private/calendar/event")
public class VEventController extends MappedCrudTenantController<Long, VCalendarEvent, VCalendarEventDto, VCalendarEventDto, VEventService> implements CalendarEventControllerAPI {

    @Autowired
    private VCalendarService calendarService;

    @Override
    public VCalendarEventDto beforeUpdate(Long id, VCalendarEventDto eventDto) {
        Optional<VCalendarEvent> optional = this.crudService().findById(TenantConstants.DEFAULT_TENANT_NAME, id);
        if (optional.isPresent()) {
            VCalendarEvent event = optional.get();
            VCalendar vCalendar = calendarService.findByTenantAndName(event.getTenant(), event.getCalendar());
            if (vCalendar == null) {
                throw new CalendarNotFoundException("with tenant/name : " + event.getTenant() + "/" + event.getCalendar());
            }

            try {
                ICalendar calendar = ICalendarBuilder.builder()
                        .icsPath(vCalendar.getIcsPath())
                        .build()
                        .calendar()
                        .load()
                        .updateEvent(IEvent.builder()
                                .uid(new Uid(eventDto.getId().toString()))
                                .name(new Name(eventDto.getName()))
                                .description(new Description(eventDto.getName()))
                                .summary(new Summary(eventDto.getName()))
                                .dtStart(new DtStart(eventDto.getStartDate().toInstant()))
                                .dtEnd(new DtEnd(eventDto.getEndDate().toInstant()))
                                .build()
                                .event())
                        .store();
            } catch (IOException e) {
                log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            } catch (ParserException e) {
                log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            }
        }
        return eventDto;
    }

    @Override
    public boolean beforeDelete(Long id) {
        Optional<VCalendarEvent> optional = this.crudService().findById(TenantConstants.DEFAULT_TENANT_NAME, id);
        if (optional.isPresent()) {
            VCalendarEvent event = optional.get();
            VCalendar vCalendar = calendarService.findByTenantAndName(event.getTenant(), event.getCalendar());
            if (vCalendar == null) {
                throw new CalendarNotFoundException("with tenant/name : " + event.getTenant() + "/" + event.getCalendar());
            }


            try {
                ICalendar calendar = ICalendarBuilder.builder()
                        .icsPath(vCalendar.getIcsPath())
                        .build()
                        .calendar()
                        .load()
                        .remove(new Uid(id.toString()))
                        .store();
            } catch (IOException e) {
                log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            } catch (ParserException e) {
                log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            }
        }

        return true;
    }

    @Override
    public ResponseEntity<VCalendarEventDto> eventByTenantAndCalendarAndCode(RequestContextDto requestContext,
                                                                             String tenant, String calendar,
                                                                             String code) {
        try {
            Optional<VCalendarEvent> event = this.crudService().findByTenantAndCalendarAndCode(tenant, calendar, code);
            if (event.isPresent()) {
                return ResponseFactory.responseOk(this.mapper().entityToDto(event.get()));
            } else {
                return ResponseFactory.responseNoContent();
            }
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<List<VCalendarEventDto>> getAllByTenantAndCalendarName(RequestContextDto requestContext,
                                                                                 String tenant, String calendar) {
        try {
            List<VCalendarEventDto> listEvent =
                    this.mapper().listEntityToDto(this.crudService().findByTenantAndCalendar(tenant, calendar));
            if (CollectionUtils.isEmpty(listEvent)) {
                return ResponseFactory.responseNoContent();
            } else {
                return ResponseFactory.responseOk(listEvent);
            }
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<VCalendarEventDto> saveEvent(RequestContextDto requestContext,
                                                       VCalendarEventDto event) {
        try {
            return ResponseFactory.responseOk(this.mapper().entityToDto(this.crudService().create(requestContext.getSenderTenant(),
                    this.mapper().dtoToEntity(event))));
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<VCalendarEventDto> updateEvent(RequestContextDto requestContext,
                                                         Long id,
                                                         VCalendarEventDto event) {
        try {
            this.crudService().findById(requestContext.getSenderTenant(), id);
            return ResponseFactory.responseOk(this.mapper().entityToDto(this.crudService().update(requestContext.getSenderTenant(),
                    this.mapper().dtoToEntity(event))));
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }
}
