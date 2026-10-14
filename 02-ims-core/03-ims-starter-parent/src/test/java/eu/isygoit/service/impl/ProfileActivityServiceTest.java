package eu.isygoit.service.impl;

import eu.isygoit.dto.data.ConnectionPageDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.DailyLoginCountDto;
import eu.isygoit.mapper.ConnectionTrackingMapper;
import eu.isygoit.model.ConnectionTracking;
import eu.isygoit.repository.ConnectionTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The profile history is read page by page / per bounded window, never as the whole connection list.
 */
@ExtendWith(MockitoExtension.class)
class ProfileActivityServiceTest {

    private static final long ACCOUNT_ID = 7L;

    @Mock
    private ConnectionTrackingRepository repository;
    @Mock
    private ConnectionTrackingMapper mapper;

    private ProfileActivityService service() {
        return new ProfileActivityService(repository, mapper);
    }

    private static Date atNoon(LocalDate day) {
        return Date.from(day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void pageSizeIsCappedAndNegativePageBecomesFirst() {
        when(repository.findPageByAccountId(eq(ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        service().getConnections(ACCOUNT_ID, -3, 10_000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findPageByAccountId(eq(ACCOUNT_ID), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(ProfileActivityService.MAX_PAGE_SIZE, captor.getValue().getPageSize());
    }

    @Test
    void zeroPageSizeBecomesOne() {
        when(repository.findPageByAccountId(eq(ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        service().getConnections(ACCOUNT_ID, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findPageByAccountId(eq(ACCOUNT_ID), captor.capture());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    void pageMetadataAndContentAreMapped() {
        ConnectionTracking first = ConnectionTracking.builder().id(1L).browser("Firefox").build();
        ConnectionTracking second = ConnectionTracking.builder().id(2L).browser("Chrome").build();
        Page<ConnectionTracking> page = new PageImpl<>(List.of(first, second), PageRequest.of(2, 2), 7);
        when(repository.findPageByAccountId(eq(ACCOUNT_ID), any(Pageable.class))).thenReturn(page);
        when(mapper.entityToDto(first)).thenReturn(ConnectionTrackingDto.builder().id(1L).browser("Firefox").build());
        when(mapper.entityToDto(second)).thenReturn(ConnectionTrackingDto.builder().id(2L).browser("Chrome").build());

        ConnectionPageDto result = service().getConnections(ACCOUNT_ID, 2, 2);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(7, result.getTotalElements());
        assertEquals(4, result.getTotalPages());
    }

    @Test
    void dailySeriesCoversEveryDayAndCountsLogins() {
        LocalDate today = LocalDate.now();
        when(repository.findLoginDatesSince(eq(ACCOUNT_ID), any(Date.class), any(Pageable.class)))
                .thenReturn(List.of(atNoon(today), atNoon(today), atNoon(today.minusDays(2))));

        List<DailyLoginCountDto> series = service().getLoginActivity(ACCOUNT_ID, 7);

        assertEquals(7, series.size());
        assertEquals(today.minusDays(6), series.get(0).getDate());
        assertEquals(today, series.get(6).getDate());
        assertEquals(2, series.get(6).getCount());
        assertEquals(1, series.get(4).getCount());
        assertEquals(0, series.get(5).getCount());
    }

    @Test
    void windowIsCappedAndTheQueryIsBounded() {
        when(repository.findLoginDatesSince(eq(ACCOUNT_ID), any(Date.class), any(Pageable.class)))
                .thenReturn(List.of());

        List<DailyLoginCountDto> series = service().getLoginActivity(ACCOUNT_ID, 100_000);

        assertEquals(ProfileActivityService.MAX_DAYS, series.size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findLoginDatesSince(eq(ACCOUNT_ID), any(Date.class), captor.capture());
        assertEquals(ProfileActivityService.MAX_CHART_LOGINS, captor.getValue().getPageSize());
        assertTrue(series.stream().allMatch(d -> d.getCount() == 0));
    }
}
