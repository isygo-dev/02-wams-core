package eu.isygoit.dto.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type File tags dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class FileTagsDto {

    private String domain;
    private String bucketName;
    private String filetName;
    private List<String> tags;
}