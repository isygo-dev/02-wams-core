package eu.isygoit.dto.request;


import eu.isygoit.helper.UrlHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Request tracking dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RequestTrackingDto {

    private String device;
    private String browser;
    private String ipOrigin;
    private String appOrigin;

    /**
     * Gets from request.
     *
     * @param request the request
     * @return the from request
     */
    public static RequestTrackingDto getFromRequest(HttpServletRequest request) {
        return RequestTrackingDto.builder()
                .device(UrlHelper.getDeviceType(request))
                .browser(UrlHelper.getBrowserType(request))
                .ipOrigin(UrlHelper.getClientIpAddress(request))
                .build();
    }
}
