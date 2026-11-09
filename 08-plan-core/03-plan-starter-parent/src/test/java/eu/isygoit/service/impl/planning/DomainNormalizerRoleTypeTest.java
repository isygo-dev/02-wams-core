package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainNormalizerRoleTypeTest {

    private static final Instant START = Instant.parse("2026-09-07T06:00:00Z");

    private final DomainNormalizer normalizer = new DomainNormalizer(new ConstraintFamilyRegistry(List.of()));

    @Test
    void infersMatchingResourceTypesForRolesWithoutExplicitTypeFilters() {
        ResourceSelector sharedSkill = selector(null);
        PlanningProblem problem = normalizer.normalize(request(List.of(
                task("teacher", List.of(new ResourceRequirement("teacher", sharedSkill, 1, null))),
                task("room", List.of(new ResourceRequirement("room", sharedSkill, 1, null))),
                task("operator", List.of(new ResourceRequirement("operator", sharedSkill, 1, null))),
                task("explicit", List.of(new ResourceRequirement("teacher", selector(List.of("room")), 1, null))))));

        assertEquals(List.of("teacher"), problem.tasks().get(0).requirements().get(0).selector().resourceTypeIds());
        assertEquals(List.of("room"), problem.tasks().get(1).requirements().get(0).selector().resourceTypeIds());
        assertEquals(List.of(), problem.tasks().get(2).requirements().get(0).selector().resourceTypeIds());
        assertEquals(List.of("room"), problem.tasks().get(3).requirements().get(0).selector().resourceTypeIds());
    }

    private static PlanningRequest request(List<Task> tasks) {
        return new PlanningRequest(new PlanningMeta(null, "2.0", "test", "en-US", "UTC"), null,
                new Horizon(START, START.plusSeconds(900), 900), null, null, null,
                List.of(resource("teacher-1", "teacher"), resource("room-1", "room")),
                tasks, null, null, null, null);
    }

    private static Task task(String id, List<ResourceRequirement> requirements) {
        return new Task(id, "lesson", null, 1, null, 1, null, null, null, requirements,
                null, null, null, null, null, null, null, null, null, null);
    }

    private static Resource resource(String id, String typeId) {
        return new Resource(id, typeId, id, null, Set.of("shared"), null, null, null, null, null,
                null, null, null);
    }

    private static ResourceSelector selector(List<String> typeIds) {
        return new ResourceSelector(null, typeIds, List.of("shared"), null, null, null);
    }
}
