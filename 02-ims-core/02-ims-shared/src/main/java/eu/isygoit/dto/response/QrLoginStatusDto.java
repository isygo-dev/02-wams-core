package eu.isygoit.dto.response;

import eu.isygoit.enums.QrLoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrLoginStatusDto {

    private QrLoginStatus status;
}
