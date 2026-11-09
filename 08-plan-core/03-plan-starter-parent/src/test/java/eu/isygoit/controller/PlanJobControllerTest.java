package eu.isygoit.controller;

import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.ControllerUtils;
import eu.isygoit.com.rest.controller.impl.CrudControllerUtils;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.dto.response.CapabilitiesDto;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.exception.ConflictException;
import eu.isygoit.exception.JobNotFoundException;
import eu.isygoit.exception.handler.PlanExceptionHandler;
import eu.isygoit.i18n.service.LocaleService;
import eu.isygoit.mapper.PlanJobMapper;
import eu.isygoit.model.PlanJob;
import eu.isygoit.service.RequestContextService;
import eu.isygoit.service.impl.PlanJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link PlanJobController} through a standalone MockMvc (no Spring context): the stack's application-context lookups
 * (service, mapper, exception handler, request context) are served by a Mockito {@link ApplicationContextService}.
 */
class PlanJobControllerTest {

    private static final String BASE = "/api/v1/private/plan/job";

    private PlanJobService service;
    private RequestContextService requestContextService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PlanJobService.class);
        requestContextService = mock(RequestContextService.class);
        when(requestContextService.getCurrentContext())
                .thenReturn(RequestContextDto.builder().senderTenant("acme").build());

        PlanJobMapper mapper = mock(PlanJobMapper.class);
        when(mapper.entityToDto(any())).thenAnswer(invocation -> {
            PlanJob job = invocation.getArgument(0);
            return PlanJobDto.builder().id(job.getId()).code(job.getCode()).tenant(job.getTenant())
                    .status(job.getStatus()).build();
        });

        PlanExceptionHandler handler = new PlanExceptionHandler(mock(AppProperties.class));
        LocaleService localeService = mock(LocaleService.class);
        when(localeService.getMessage(anyString(), any(Locale.class))).thenAnswer(i -> i.getArgument(0));
        handler.setLocaleService(localeService);

        ApplicationContextService context = mock(ApplicationContextService.class);
        when(context.getBean(PlanJobService.class)).thenReturn(Optional.of(service));
        when(context.getBean(PlanJobMapper.class)).thenReturn(Optional.of(mapper));
        when(context.getBean(PlanExceptionHandler.class)).thenReturn(Optional.of(handler));
        when(context.getBean(RequestContextService.class)).thenReturn(Optional.of(requestContextService));

        eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler stackHandler =
                mock(eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler.class);
        when(stackHandler.getApplicationContextService()).thenReturn(context);

        PlanJobController controller = new PlanJobController();
        ReflectionTestUtils.setField(controller, ControllerUtils.class, "controllerExceptionHandler", stackHandler, null);
        ReflectionTestUtils.setField(controller, CrudControllerUtils.class, "controllerExceptionHandler", stackHandler, null);
        ReflectionTestUtils.setField(controller, "applicationContextService", context);

        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(handler).build();
    }

    private static PlanJob job(long id, String code) {
        return PlanJob.builder().id(id).code(code).tenant("acme").status(IEnumJobStatusCode.Types.CREATED).build();
    }

    // region tenant resolution

    @Test
    void missingTenant_isRejectedWith400TenantRequired() throws Exception {
        when(requestContextService.getCurrentContext())
                .thenReturn(RequestContextDto.builder().senderTenant(" ").build());

        mvc.perform(get(BASE + "/dashboard"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"))
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(post(BASE + "/1/cancel")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void tenantIsAlwaysTheCallersAndNeverTheBodys() throws Exception {
        when(service.submit(eq("acme"), any())).thenReturn(job(1, "PLN-1"));

        mvc.perform(post(BASE + "/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenant\":\"someone-else\"}"))
                .andExpect(status().isAccepted());

        verify(service).submit(eq("acme"), any());
    }

    // endregion

    @Test
    void submit_returns202WithTheJob() throws Exception {
        when(service.submit(eq("acme"), any())).thenReturn(job(42, "PLN-000042"));

        mvc.perform(post(BASE + "/submit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.code").value("PLN-000042"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void submit_invalidBodyIs400InvalidRequest() throws Exception {
        mvc.perform(post(BASE + "/submit").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void notFound_is404ApiError() throws Exception {
        when(service.getResult("acme", 5L)).thenThrow(new JobNotFoundException(5L));

        mvc.perform(get(BASE + "/5/result"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("plan.error.job.not.found"))
                .andExpect(jsonPath("$.path").value(BASE + "/5/result"));
    }

    @Test
    void cancelAndStop_statusCodes() throws Exception {
        mvc.perform(post(BASE + "/3/cancel")).andExpect(status().isOk());
        mvc.perform(post(BASE + "/3/stop")).andExpect(status().isAccepted());
        verify(service).cancel("acme", 3L);
        verify(service).stop("acme", 3L);

        doThrow(new ConflictException("already started")).when(service).cancel("acme", 4L);
        mvc.perform(post(BASE + "/4/cancel")).andExpect(status().isConflict());
    }

    @Test
    void updateRequest_returnsTheJob() throws Exception {
        when(service.updateRequest(eq("acme"), eq(8L), any())).thenReturn(job(8, "PLN-8"));

        mvc.perform(put(BASE + "/8/request").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
    }

    @Test
    void validateAndCapabilities_delegate() throws Exception {
        when(service.validate(any(), any())).thenReturn(new ValidationReportDto());
        when(service.capabilities()).thenReturn(new CapabilitiesDto());

        mvc.perform(post(BASE + "/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(get(BASE + "/capabilities")).andExpect(status().isOk());
        verify(service).validate(any(), isNull());
    }

    @Test
    void validate_passesTheEngineOverride() throws Exception {
        when(service.validate(any(), any())).thenReturn(new ValidationReportDto());

        mvc.perform(post(BASE + "/validate").param("engine", "timefold")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(service).validate(any(), eq("timefold"));
    }

    // region export

    @Test
    void exportCsv_hasAttachmentFilenameAndCsvContentType() throws Exception {
        when(service.getOwned("acme", 7L)).thenReturn(job(7, "PLN-000007"));
        when(service.exportResult("acme", 7L, "csv", null)).thenReturn("a;b\n".getBytes());

        mvc.perform(get(BASE + "/7/result/export").param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"plan-job-PLN-000007.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().bytes("a;b\n".getBytes()));
    }

    @Test
    void exportDefaultsToJson() throws Exception {
        when(service.getOwned("acme", 7L)).thenReturn(job(7, "PLN-000007"));
        when(service.exportResult("acme", 7L, "json", null)).thenReturn("{}".getBytes());

        mvc.perform(get(BASE + "/7/result/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"plan-job-PLN-000007.json\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void exportCsvSection_isPassedLowerCasedAndNamedInTheFile() throws Exception {
        when(service.getOwned("acme", 7L)).thenReturn(job(7, "PLN-000007"));
        when(service.exportResult("acme", 7L, "csv", "violations")).thenReturn("h\n".getBytes());
        when(service.exportResult("acme", 7L, "csv", "assignments")).thenReturn("a\n".getBytes());

        mvc.perform(get(BASE + "/7/result/export").param("format", "csv").param("section", " Violations "))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"plan-job-PLN-000007-violations.csv\""));
        mvc.perform(get(BASE + "/7/result/export").param("format", "csv").param("section", "assignments"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"plan-job-PLN-000007.csv\""));
    }

    @Test
    void exportUnknownSection_isTheServicesInvalidRequest() throws Exception {
        when(service.getOwned("acme", 7L)).thenReturn(job(7, "PLN-000007"));
        when(service.exportResult("acme", 7L, "csv", "everything"))
                .thenThrow(new eu.isygoit.exception.InvalidRequestException("INVALID_EXPORT_SECTION", "unknown"));

        mvc.perform(get(BASE + "/7/result/export").param("format", "csv").param("section", "everything"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EXPORT_SECTION"));
    }

    @Test
    void exportUnsupportedFormat_is400() throws Exception {
        mvc.perform(get(BASE + "/7/result/export").param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_EXPORT_FORMAT"));
        verifyNoInteractions(service);
    }

    // endregion

    // region limits

    @Test
    void logs_defaultLimitIs100() throws Exception {
        when(service.getLogs(anyString(), anyLong(), any(), any(), any(), anyInt())).thenReturn(List.of());

        mvc.perform(get(BASE + "/1/logs")).andExpect(status().isOk());

        verify(service).getLogs("acme", 1L, null, null, null, 100);
    }

    @Test
    void logs_limitIsClampedTo1And1000() throws Exception {
        when(service.getLogs(anyString(), anyLong(), any(), any(), any(), anyInt())).thenReturn(List.of());

        mvc.perform(get(BASE + "/1/logs").param("limit", "50000")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/1/logs").param("limit", "0")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/1/logs").param("limit", "-5")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/1/logs").param("limit", "250")).andExpect(status().isOk());

        verify(service).getLogs("acme", 1L, null, null, null, 1000);
        verify(service, org.mockito.Mockito.times(2)).getLogs("acme", 1L, null, null, null, 1);
        verify(service).getLogs("acme", 1L, null, null, null, 250);
    }

    @Test
    void logs_passesRangeAndLevel() throws Exception {
        when(service.getLogs(anyString(), anyLong(), any(), any(), any(), anyInt())).thenReturn(List.of());

        mvc.perform(get(BASE + "/1/logs")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-02T00:00:00Z")
                        .param("minLevel", IEnumLogLevel.Types.values()[0].name()))
                .andExpect(status().isOk());

        verify(service).getLogs("acme", 1L, Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"), IEnumLogLevel.Types.values()[0], 100);
    }

    @Test
    void dashboard_defaultIs50AndClampedTo1And500() throws Exception {
        mvc.perform(get(BASE + "/dashboard")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/dashboard").param("limit", "99999")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/dashboard").param("limit", "0")).andExpect(status().isOk());

        verify(service).dashboard("acme", 50);
        verify(service).dashboard("acme", 500);
        verify(service).dashboard("acme", 1);
    }

    // endregion

    // region create / update bypass

    @Test
    void inheritedCreate_isRejectedWith405AndNeverReachesTheService() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        verifyNoInteractions(service);
    }

    @Test
    void inheritedCreateBatch_isRejectedWith405() throws Exception {
        mvc.perform(post(BASE + "/batch").contentType(MediaType.APPLICATION_JSON).content("[{\"code\":\"X\"}]"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test
    void inheritedUpdate_isRejectedWith405() throws Exception {
        mvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\"}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    // endregion
}
