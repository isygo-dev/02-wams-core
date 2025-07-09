package eu.isygoit.dto.data;

import eu.isygoit.dto.IFileUploadDto;
import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

/**
 * The type Template dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MsgTemplateDto extends AbstractAuditableDto<Long> implements IFileUploadDto {

    private String tenant;
    private String name;
    private String code;
    private String description;
    private String path;
    private String fileName;
    private String originalFileName;
    private MultipartFile file;
    @Builder.Default
    private IEnumLanguage.Types language = IEnumLanguage.Types.EN;
}
