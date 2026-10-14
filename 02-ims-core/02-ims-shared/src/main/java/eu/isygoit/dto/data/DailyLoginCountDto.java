package eu.isygoit.dto.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Number of tracked logins on one calendar day (used for the login-activity chart).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLoginCountDto {

    private LocalDate date;
    private long count;
}
