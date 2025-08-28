package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.enums.IEnumRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The type Role permission dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RolePermissionDto extends AuditableDto<Long> {

    private Long id;
    private String serviceName;
    private String objectName;
    private Boolean read;
    private Boolean write;
    private Boolean delete;

    /**
     * Gets http request.
     *
     * @return the http request
     */
    public List<IEnumRequest.Types> getHTTPRequest() {
        List<IEnumRequest.Types> rqTypes = new ArrayList<>();
        if (getRead()) rqTypes.add(IEnumRequest.Types.GET);
        if (getDelete()) rqTypes.add(IEnumRequest.Types.DELETE);
        if (getWrite()) rqTypes.addAll(Arrays.asList(IEnumRequest.Types.PUT, IEnumRequest.Types.POST));
        return rqTypes;
    }
}
