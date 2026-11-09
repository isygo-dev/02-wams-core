package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Sequence-dependent setup/transition times of a resource ({@code Resource.setupMatrixId}).
 * {@code setup(i -> j)} is the idle time the resource needs between the END of i and the START of j when j is the
 * immediate successor of i on that resource. The first task on a resource has no setup. Task classes come from
 * {@code Task.setupClass}; a task without class has setup 0 to and from everything.
 *
 * @param id             unique matrix id
 * @param defaultTicks   setup between two different classes with no explicit entry (default 0)
 * @param sameClassTicks setup between two tasks of the same class (default 0)
 * @param entries        sparse overrides
 */
public record SetupMatrix(
        String id,
        int defaultTicks,
        int sameClassTicks,
        List<SetupEntry> entries) {

    public SetupMatrix {
        entries = CollectionCopies.list(entries);
    }
}
