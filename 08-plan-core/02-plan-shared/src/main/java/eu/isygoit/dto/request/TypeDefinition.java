package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * A declared type: its id and the attribute names the UI offers for it.
 *
 * @param id         the type id referenced by {@code typeId} fields
 * @param attributes attribute names, default empty
 */
public record TypeDefinition(String id, List<String> attributes) {

    public TypeDefinition {
        attributes = CollectionCopies.list(attributes);
    }
}
