package eu.isygoit.repository;

import eu.isygoit.model.Annex;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * The interface Annex repository.
 */
public interface AnnexRepository extends JpaPagingAndSortingTenantAssignableRepository<Annex, Long> {
    /**
     * Find by table code list.
     *
     * @param code the code
     * @return the list
     */
    List<Annex> findByTableCode(String code);

    /**
     * Find by table code list.
     *
     * @param code      the code
     * @param reference
     * @return the list
     */
    List<Annex> findByTableCodeAndReference(String code, String reference);

    /**
     * Returns every distinct {@code tableCode} currently stored for the tenant,
     * sorted alphabetically. Used to populate {@code tableCode} filter pickers.
     *
     * @return the ordered list of distinct table codes (never {@code null})
     */
    @Query("SELECT DISTINCT a.tableCode FROM Annex a "
            + "WHERE a.tableCode IS NOT NULL AND a.tableCode <> '' "
            + "ORDER BY a.tableCode ASC")
    List<String> findDistinctTableCodes();
}
