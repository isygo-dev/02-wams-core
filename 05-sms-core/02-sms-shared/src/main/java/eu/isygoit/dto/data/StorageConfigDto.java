package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * The type Storage config dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class StorageConfigDto extends AbstractAuditableDto<Long> {

    private Long id;
    private String tenant;
    private IEnumStorage.Types type;
    private String userName;
    private String password;
    private String url;
}
