package eu.isygoit.mapper;

import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.model.PlanJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

/**
 * Mapper for {@link PlanJob} ↔ {@link PlanJobDto}.
 * <p>
 * The entity stores the request, the result and the diagnostics as jsonb strings; the DTO carries them typed. The
 * conversion is done by {@link PlanJobJsonConverter} (Spring-managed {@code ObjectMapper}, null-safe) through the
 * qualified mappings below.
 * <p>
 * In the DTO → entity direction only the {@code id} is taken (needed by the inherited batch delete); every other
 * field (code, tenant, status, timestamps, json columns, audit, version) is owned by the job lifecycle and ignored.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring",
        uses = PlanJobJsonConverter.class)
public interface PlanJobMapper extends EntityMapper<PlanJob, PlanJobDto> {

    @Override
    @Mapping(target = "request", source = "requestJson", qualifiedByName = "requestFromJson")
    @Mapping(target = "result", source = "resultJson", qualifiedByName = "resultFromJson")
    @Mapping(target = "diagnostics", source = "diagnosticsJson", qualifiedByName = "diagnosticsFromJson")
    PlanJobDto entityToDto(PlanJob object);

    @Override
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "clientRef", ignore = true)
    @Mapping(target = "engine", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "dispatchedAt", ignore = true)
    @Mapping(target = "dispatchAttempts", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "errorCode", ignore = true)
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "requestJson", ignore = true)
    @Mapping(target = "resultJson", ignore = true)
    @Mapping(target = "diagnosticsJson", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    PlanJob dtoToEntity(PlanJobDto object);
}
