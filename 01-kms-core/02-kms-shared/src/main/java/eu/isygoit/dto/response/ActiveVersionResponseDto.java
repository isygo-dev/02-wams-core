package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveVersionResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long keyId;
    private String versionId;
}