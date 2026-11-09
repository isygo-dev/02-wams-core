package eu.isygoit.mapper;

import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.model.PlanJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

/**
 * Mapper for {@link PlanJob} ↔ {@link PlanJobMinDto}.
 * <p>
 * Lightweight representation (lists, lookups, dashboard): the json columns ({@code requestJson}, {@code resultJson},
 * {@code diagnosticsJson}) are never read nor written. In the DTO → entity direction only the {@code id} is taken
 * (needed by the inherited batch delete); every other field is owned by the job lifecycle and ignored.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface PlanJobMinMapper extends EntityMapper<PlanJob, PlanJobMinDto> {

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
    PlanJob dtoToEntity(PlanJobMinDto object);
}
