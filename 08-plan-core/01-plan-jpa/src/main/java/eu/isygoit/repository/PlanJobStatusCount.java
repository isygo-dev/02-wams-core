package eu.isygoit.repository;

import eu.isygoit.enums.IEnumJobStatusCode;

/**
 * Projection of the grouped "select status, count(*)" dashboard query.
 */
public interface PlanJobStatusCount {
    IEnumJobStatusCode.Types getStatus();

    long getTotal();
}
