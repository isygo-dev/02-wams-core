package eu.isygoit.service.impl;

import eu.isygoit.dto.data.ConnectionPageDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.DailyLoginCountDto;
import eu.isygoit.mapper.ConnectionTrackingMapper;
import eu.isygoit.model.ConnectionTracking;
import eu.isygoit.repository.ConnectionTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Read side of the profile page's login history. Everything is queried page by page (or for a bounded time
 * window) instead of loading the account's whole, ever-growing connection list.
 */
@Service
@RequiredArgsConstructor
public class ProfileActivityService {

    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_DAYS = 90;
    /**
     * Safety cap on the rows read to build the daily chart.
     */
    static final int MAX_CHART_LOGINS = 5000;

    private final ConnectionTrackingRepository connectionTrackingRepository;
    private final ConnectionTrackingMapper connectionTrackingMapper;

    @Transactional(readOnly = true)
    public ConnectionPageDto getConnections(Long accountId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Page<ConnectionTracking> result = connectionTrackingRepository
                .findPageByAccountId(accountId, PageRequest.of(safePage, safeSize));
        List<ConnectionTrackingDto> content = result.getContent().stream()
                .map(connectionTrackingMapper::entityToDto)
                .toList();
        return ConnectionPageDto.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * The newest login with the total count (a page of one row).
     */
    @Transactional(readOnly = true)
    public Page<ConnectionTracking> latestConnection(Long accountId) {
        return connectionTrackingRepository.findPageByAccountId(accountId, PageRequest.of(0, 1));
    }

    /**
     * Logins per day for the last {@code days} days (today included), oldest first. Days without a login are
     * present with a zero count so the chart timeline stays continuous.
     */
    @Transactional(readOnly = true)
    public List<DailyLoginCountDto> getLoginActivity(Long accountId, int days) {
        int safeDays = Math.min(MAX_DAYS, Math.max(1, days));
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate from = today.minusDays(safeDays - 1L);
        Date since = Date.from(from.atStartOfDay(zone).toInstant());

        Map<LocalDate, Long> counts = new TreeMap<>();
        for (Date login : connectionTrackingRepository
                .findLoginDatesSince(accountId, since, PageRequest.of(0, MAX_CHART_LOGINS))) {
            if (login != null) {
                counts.merge(login.toInstant().atZone(zone).toLocalDate(), 1L, Long::sum);
            }
        }

        List<DailyLoginCountDto> series = new ArrayList<>(safeDays);
        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
            series.add(new DailyLoginCountDto(day, counts.getOrDefault(day, 0L)));
        }
        return series;
    }
}
