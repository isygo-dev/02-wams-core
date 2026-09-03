package eu.isygoit.service;

public interface DashboardService<T> {

    /**
     * Returns aggregated dashboard statistics.
     * @return DashboardStatsDto containing all KPIs
     */
     T getDashboardStats();
}