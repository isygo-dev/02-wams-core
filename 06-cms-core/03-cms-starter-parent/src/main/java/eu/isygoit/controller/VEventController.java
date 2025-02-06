package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.api.CalendarEventControllerAPI;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.VCalendarEventDto;
import eu.isygoit.exception.CalendarNotFoundException;
import eu.isygoit.exception.handler.CmsExceptionHandler;
import eu.isygoit.jasycal.ICalendarBuilder;
import eu.isygoit.jasycal.IEvent;
import eu.isygoit.mapper.VCalendarEventMapper;
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
@CtrlDef(handler = CmsExceptionHandler.class, mapper = VCalendarEventMapper.class, minMapper = VCalendarEventMapper.class, service = VEventService.class)
@RequestMapping(path = "/api/v1/private/calendar/event")
public class VEventController extends MappedCrudController<Long, VCalendarEvent, VCalendarEventDto, VCalendarEventDto, VEventService> implements CalendarEventControllerAPI {

    @Autowired
    private VCalendarService calendarService;

    @Override
    public VCalendarEventDto beforeUpdate(Long id, VCalendarEventDto eventDto) {
        this.crudService().findById(id).ifPresent(event -> {
            calendarService.findByDomainAndName(event.getDomain(), event.getCalendar())
                    .ifPresentOrElse(vCalendar -> {
                                try {
                                    ICalendarBuilder.builder()
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
                            },
                            () -> {
                                throw new CalendarNotFoundException("with domain/name : " + event.getDomain() + "/" + event.getCalendar());
                            });
        });

        return eventDto;
    }

    @Override
    public boolean beforeDelete(Long id) {
        this.crudService().findById(id).ifPresent(event -> {
            calendarService.findByDomainAndName(event.getDomain(), event.getCalendar())
                    .ifPresentOrElse(vCalendar -> {
                                try {
                                    ICalendarBuilder.builder()
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
                            },
                            () -> {
                                throw new CalendarNotFoundException("with domain/name : " + event.getDomain() + "/" + event.getCalendar());
                            });

        });

        return true;
    }

    @Override
    public ResponseEntity<VCalendarEventDto> eventByDomainAndCalendarAndCode(RequestContextDto requestContext,
                                                                             String domain, String calendar,
                                                                             String code) {
        try {
            Optional<VCalendarEvent> event = this.crudService().findByDomainAndCalendarAndCode(domain, calendar, code);
            if (event.isPresent()) {
                return ResponseFactory.ResponseOk(this.mapper().entityToDto(event.get()));
            } else {
                return ResponseFactory.ResponseNoContent();
            }
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<List<VCalendarEventDto>> getAllByDomainAndCalendarName(RequestContextDto requestContext,
                                                                                 String domain, String calendar) {
        try {
            List<VCalendarEventDto> listEvent =
                    this.mapper().listEntityToDto(this.crudService().findByDomainAndCalendar(domain, calendar));
            if (CollectionUtils.isEmpty(listEvent)) {
                return ResponseFactory.ResponseNoContent();
            } else {
                return ResponseFactory.ResponseOk(listEvent);
            }
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<VCalendarEventDto> saveEvent(//RequestContextDto requestContext,
                                                       VCalendarEventDto event) {
        try {
            return ResponseFactory.ResponseOk(this.mapper().entityToDto(this.crudService().create((this.mapper().dtoToEntity(event)))));
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }

    @Override
    public ResponseEntity<VCalendarEventDto> updateEvent(//RequestContextDto requestContext,
                                                         Long id,
                                                         VCalendarEventDto event) {
        try {
            this.crudService().findById(id);
            return ResponseFactory.ResponseOk(this.mapper().entityToDto(this.crudService().update((this.mapper().dtoToEntity(event)))));
        } catch (Exception ex) {
            return getBackExceptionResponse(ex);
        }
    }
}
