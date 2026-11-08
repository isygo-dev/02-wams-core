package eu.isygoit.plan.api;

import java.util.List;

public record DomainDefinition(
        List<TypeDefinition> resourceTypes,
        List<TypeDefinition> taskTypes,
        List<TypeDefinition> slotTypes) {
}
