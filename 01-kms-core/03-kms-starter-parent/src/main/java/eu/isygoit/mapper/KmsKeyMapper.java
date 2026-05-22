package eu.isygoit.mapper;

import eu.isygoit.dto.KmsDtos.KeyDescriptionResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
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
    KeyDescriptionResponse toKeyMetadataResponseDto(KmsKey kmsKey);

    /**
     * Convert Page of KmsKey entities to ListKeysResponseDto
     */
    default ListKeysResponse toListKeysResponseDto(Page<KmsKey> page) {
        List<ListKeysResponse.KeyEntry> keys = page.getContent().stream()
                .map(key -> ListKeysResponse.KeyEntry.builder()
                        .keyId(key.getKeyId())
                        .alias(key.getPrimaryKeyAlias())
                        .status(key.getKeyStatus())
                        .build())
                .toList();

        return ListKeysResponse.builder()
                .keys(keys)
                .nextToken(page.hasNext() ? String.valueOf(page.getNumber() + 1) : null)
                .build();
    }

}

