package eu.isygoit.dto.data;


import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.IImageUploadDto;
import eu.isygoit.dto.extendable.DomainModelDto;
import eu.isygoit.model.IDomainAssignable;
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
public class DomainDto extends DomainModelDto<Long> implements IDomainAssignable, IImageUploadDto {

    private String domain;
    private String code;
    private String email;
    private String phone;
    private String lnk_facebook;
    private String lnk_linkedin;
    private String lnk_xing;
    private AddressDto address;
    private String imagePath;
}
