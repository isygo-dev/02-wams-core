package eu.isygoit.dto.data;

import eu.isygoit.dto.IImageUploadDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Application dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ApplicationDto extends AbstractAuditableDto<Long> implements IImageUploadDto {

    private Long id;
    private String tenant;
    private String code;
    @NotEmpty
    private String name;
    @NotEmpty
    private String title;
    private String description;
    @NotEmpty
    @Builder.Default
    private String category = "PRM Store";
    @NotEmpty
    private String url;
    private Integer order;
    private String imagePath;

    @Builder.Default
    private IEnumEnabledBinaryStatus.Types adminStatus = IEnumEnabledBinaryStatus.Types.ENABLED;

    //App authorization token
    private TokenResponseDto token;
}
