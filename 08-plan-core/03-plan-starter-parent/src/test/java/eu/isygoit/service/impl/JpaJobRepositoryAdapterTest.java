package eu.isygoit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.model.PlanJob;
import eu.isygoit.plan.domain.Diagnostics;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.repository.PlanJobRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaJobRepositoryAdapterTest {
    @Test
    void returnsPersistedDiagnosticsForInfeasibleJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        Diagnostics diagnostics = new Diagnostics(
                "No feasible assignment", List.of("task-1"), Map.of("reason", "capacity"));
        ObjectMapper objectMapper = new ObjectMapper();
        PlanJob job = new PlanJob();
        job.setId(jobId);
        job.setStatus(IEnumJobStatusCode.Types.INFEASIBLE);
        job.setDiagnosticsJson(objectMapper.writeValueAsString(diagnostics));
        PlanJobRepository repository = mock(PlanJobRepository.class);
        when(repository.findById(jobId)).thenReturn(Optional.of(job));

        SolveResult result = new JpaJobRepositoryAdapter(repository, objectMapper)
                .findResult(jobId)
                .orElseThrow();

        assertEquals(IEnumSolverStatus.Types.INFEASIBLE, result.status());
        assertNull(result.solution());
        assertEquals(diagnostics, result.diagnostics());
    }
}
