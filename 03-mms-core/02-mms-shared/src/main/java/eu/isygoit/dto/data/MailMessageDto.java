package eu.isygoit.dto.data;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.isygoit.deserializer.UuidDeserializer;
import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumMsgTemplateName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Mail message dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MailMessageDto extends AbstractAuditableDto<UUID> {

    @JsonDeserialize(using = UuidDeserializer.class)
    private UUID id;

    private String domain;
    private IEnumMsgTemplateName.Types templateName;
    private boolean returnDelivered;
    private boolean returnRead;
    private String subject;
    private String toAddr;
    private String ccAddr;
    private String bccAddr;
    private String body;
    private Boolean sent;

    private List<MultipartFile> resources;
    private String variables;

    /**
     * Gets variables as string.
     *
     * @param map the map
     * @return the variables as string
     * @throws JsonProcessingException the json processing exception
     */
    public static String getVariablesAsString(Map<String, String> map) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }

    /**
     * Gets variables as map.
     *
     * @param json the json
     * @return the variables as map
     * @throws JsonProcessingException the json processing exception
     */
    public HashMap<String, String> getVariablesAsMap(String json) throws JsonProcessingException {
        if (StringUtils.hasText(json)) {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, String>> typeRef =
                    new TypeReference<HashMap<String, String>>() {
                    };
            return mapper.readValue(json, typeRef);
        } else {
            return new HashMap<>();
        }
    }
}
