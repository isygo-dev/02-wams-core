package eu.isygoit.mapper;

import eu.isygoit.dto.KmsDtos.KeyDescriptionResponseDto;
import eu.isygoit.dto.KmsDtos.ListKeysResponseDto;
import eu.isygoit.model.KmsKey;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * MapStruct mapper for KMS Key entity
 */
@Mapper
public interface KmsKeyMapper {

    KmsKeyMapper INSTANCE = Mappers.getMapper(KmsKeyMapper.class);

    /**
     * Convert KmsKey entity to KeyMetadataResponseDto
     */
    KeyDescriptionResponseDto toKeyMetadataResponseDto(KmsKey kmsKey);

    /**
     * Convert Page of KmsKey entities to ListKeysResponseDto
     */
    default ListKeysResponseDto toListKeysResponseDto(Page<KmsKey> page) {
        List<ListKeysResponseDto.KeySummaryDto> keys = (List<ListKeysResponseDto.KeySummaryDto>) page.getContent().stream()
                .map(key -> ListKeysResponseDto.KeySummaryDto.builder()
                        .keyId(key.getKeyId())
                        .alias(key.getPrimaryKeyAlias())
                        .status(key.getKeyStatus())
                        .build())
                .toList();

        return ListKeysResponseDto.builder()
                .keys(keys)
                .nextToken(page.hasNext() ? String.valueOf(page.getNumber() + 1) : null)
                .build();
    }

}

