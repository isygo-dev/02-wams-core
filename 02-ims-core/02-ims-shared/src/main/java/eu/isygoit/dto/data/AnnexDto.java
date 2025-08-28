package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.enums.IEnumLanguage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Annex dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AnnexDto extends AuditableDto<Long> {

    private Long id;
    private String tenant;
    @NotEmpty
    private String tableCode;
    @NotNull
    private IEnumLanguage.Types language;
    @NotEmpty
    private String value;

    private String description;

    private String reference;

    private Integer annexOrder;
}
