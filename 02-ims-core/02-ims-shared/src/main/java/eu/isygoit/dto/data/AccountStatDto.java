package eu.isygoit.dto.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * The type Account stat dto.
 * Statistics for a single account.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccountStatDto {

    private LocalDateTime createDate;
    private LocalDateTime lastLogin;
    private Integer totalConnections;
    private Integer roleCount;
    private Integer totalPermissions;
    private Boolean isActive;
    private String accountStatus;
    private String adminStatus;
}