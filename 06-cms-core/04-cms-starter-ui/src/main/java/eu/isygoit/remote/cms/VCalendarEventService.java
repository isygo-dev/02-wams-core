package eu.isygoit.remote.cms;

import eu.isygoit.api.CalendarEventServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "calendar-service",
        contextId = "vcalendarEvents", path = "/api/v1/private/calendar/event")
public interface VCalendarEventService extends CalendarEventServiceApi {
}
