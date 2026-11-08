package eu.isygoit.plan.configuration;

import eu.isygoit.plan.engine.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
public class PlanningEngineConfiguration {
    @Bean
    public ConstraintRegistry constraintRegistry(Collection<ConstraintHandler> handlers) {
        return new ConstraintRegistry(handlers);
    }

    @Bean
    public ObjectiveRegistry objectiveRegistry(Collection<ObjectiveHandler> handlers) {
        return new ObjectiveRegistry(handlers);
    }

    @Bean
    public SolverRegistry solverRegistry(Collection<SolverEngine> engines) {
        return new SolverRegistry(engines);
    }
}
