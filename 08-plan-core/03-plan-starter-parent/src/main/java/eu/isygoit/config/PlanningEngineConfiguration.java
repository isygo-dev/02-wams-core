package eu.isygoit.config;

import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.CpSatProperties;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.CpSatContributorRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.registry.TimefoldContributorRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import eu.isygoit.solver.timefold.TimefoldObjectiveContributor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

/**
 * Wiring of the Engine v2 extension points (design 5.7).
 * <p>
 * The four registries and the {@link SolverRegistry} are deliberately NOT {@code @Component}s: they are plain
 * immutable classes built here from the collections of beans Spring discovers ({@link ConstraintFamily},
 * {@link ObjectiveFamily}, the CP-SAT and Timefold contributors, {@link SolverEngine}). Adding a constraint family,
 * an objective or a contributor is therefore "add a {@code @Component}": this class is never edited. A duplicate or
 * blank type fails the application start (the registries fail fast with a message naming both classes).
 * <p>
 * Also binds the configuration properties {@link PlanLimitsProperties} ({@code app.plan.limits.*}) and
 * {@link CpSatProperties} ({@code app.plan.cpsat.*}); both carry safe defaults so the service starts without any
 * configuration. Thread-safety: the beans are immutable singletons.
 */
@Configuration
@EnableConfigurationProperties({PlanLimitsProperties.class, CpSatProperties.class})
public class PlanningEngineConfiguration {

    /**
     * @param families every {@link ConstraintFamily} bean
     * @return the registry of constraint families keyed by type
     */
    @Bean
    public ConstraintFamilyRegistry constraintFamilyRegistry(Collection<ConstraintFamily> families) {
        return new ConstraintFamilyRegistry(families);
    }

    /**
     * @param families every {@link ObjectiveFamily} bean
     * @return the registry of objective families keyed by type
     */
    @Bean
    public ObjectiveFamilyRegistry objectiveFamilyRegistry(Collection<ObjectiveFamily> families) {
        return new ObjectiveFamilyRegistry(families);
    }

    /**
     * @param constraints every CP-SAT constraint contributor bean
     * @param objectives  every CP-SAT objective contributor bean
     * @return the registry of CP-SAT contributors
     */
    @Bean
    public CpSatContributorRegistry cpSatContributorRegistry(
            Collection<CpSatConstraintContributor> constraints,
            Collection<CpSatObjectiveContributor> objectives) {
        return new CpSatContributorRegistry(constraints, objectives);
    }

    /**
     * @param constraints every Timefold constraint contributor bean
     * @param objectives  every Timefold objective contributor bean
     * @return the registry of Timefold contributors
     */
    @Bean
    public TimefoldContributorRegistry timefoldContributorRegistry(
            Collection<TimefoldConstraintContributor> constraints,
            Collection<TimefoldObjectiveContributor> objectives) {
        return new TimefoldContributorRegistry(constraints, objectives);
    }

    /**
     * @param engines every {@link SolverEngine} bean
     * @return the registry of engines keyed by canonical id (aliases resolved)
     */
    @Bean
    public SolverRegistry solverRegistry(Collection<SolverEngine> engines) {
        return new SolverRegistry(engines);
    }
}
