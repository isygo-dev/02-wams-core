package eu.isygoit.dto.response;

import eu.isygoit.dto.data.TagDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListTagsResponseDto {
    private List<TagDto> tags;
}