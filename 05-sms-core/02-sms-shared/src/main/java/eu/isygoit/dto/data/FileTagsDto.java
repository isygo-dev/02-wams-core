package eu.isygoit.dto.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * The type File tags dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class FileTagsDto {

    private String tenant;
    private String bucketName;
    private String path;
    private String filetName;
    private Map<String, String> tags;
}