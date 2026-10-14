package eu.isygoit.dto.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One page of an account's tracked login sessions, newest first. A small purpose-built page type is used
 * instead of Spring's {@code Page} so it serialises cleanly through Feign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPageDto {

    private List<ConnectionTrackingDto> content;
    /**
     * Zero-based page index.
     */
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
