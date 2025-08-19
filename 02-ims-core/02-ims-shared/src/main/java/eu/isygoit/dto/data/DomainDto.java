package eu.isygoit.dto.data;


import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.IImageUploadDto;
import eu.isygoit.dto.extendable.TenantModelDto;
import eu.isygoit.model.ITenantAssignable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Domain dto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DomainDto extends TenantModelDto<Long> implements ITenantAssignable, IImageUploadDto {

    private Long id;
    private String tenant;
    private String code;
    private String email;
    private String phone;
    private String lnk_facebook;
    private String lnk_linkedin;
    private String lnk_xing;
    private AddressDto address;
    private String imagePath;
}
