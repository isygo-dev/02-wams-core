package eu.isygoit.dto.data;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * The type Account global stat dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccountGlobalStatDto implements Serializable {

    private Long totalCount;
    private Long activeCount;
    private Long confirmedCount; // user has loggedIn at least one time
    private Long adminsCount;
}
