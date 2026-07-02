package eu.isygoit.remote.cms;

import eu.isygoit.api.VCalendarServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "calendar-service", contextId = "vcalendar", path = "/api/v1/private/calendar")
public interface VCalendarService extends VCalendarServiceApi {

}