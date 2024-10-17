package eu.isygoit.dto.data;

import eu.isygoit.dto.IImageUploadDto;
import eu.isygoit.enums.IEnumLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Account dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccountDto extends MinAccountDto implements IImageUploadDto {

    private IEnumLanguage.Types language;
    private List<RoleInfoDto> roleInfo;
    private String phoneNumber;
    private List<ConnectionTrackingDto> connectionTracking;
}
