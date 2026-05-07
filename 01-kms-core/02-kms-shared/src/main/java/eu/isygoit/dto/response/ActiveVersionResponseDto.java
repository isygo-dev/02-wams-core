package eu.isygoit.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ActiveVersionResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long keyId;
    private String versionId;

    public ActiveVersionResponseDto() {
    }

    public ActiveVersionResponseDto(Long keyId, String versionId) {
        this.keyId = keyId;
        this.versionId = versionId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(Long keyId) {
        this.keyId = keyId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}