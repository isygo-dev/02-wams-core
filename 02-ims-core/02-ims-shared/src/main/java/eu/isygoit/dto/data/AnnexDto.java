package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableIdAssignableDto;
import eu.isygoit.enums.IEnumLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Annex dto.
 *
 * <p>Required string fields use {@link NotBlank} so whitespace-only input is
 * rejected; {@code translated} is required because {@code AnnexModel}'s
 * column is {@code nullable = false}. The entity's {@code getTranslated()}
 * fallback materialises a value when the stored one is blank, so a DTO
 * round-tripped from a legacy row always arrives with a non-blank
 * {@code translated}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AnnexDto extends AuditableIdAssignableDto<Long> {

    private Long id;
    private String tenant;

    @NotBlank
    private String tableCode;

    @NotBlank
    private String value;

    @NotNull
    private IEnumLanguage.Types language;

    @NotBlank
    private String translated;

    private String description;

    private String reference;

    private Integer annexOrder;
}