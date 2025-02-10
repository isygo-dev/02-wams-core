package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.Annex;

import java.util.List;

/**
 * The interface Annex service.
 */
public interface IAnnexService extends ICrudServiceMethod<Long, Annex> {

    /**
     * Find annex by code list.
     *
     * @param code the code
     * @return the list
     */
    List<Annex> getByTableCode(String code);

    /**
     * Find annex by code and ref list.
     *
     * @param code      the code
     * @param reference the reference
     * @return the list
     */
    List<Annex> getByTableCodeAndRef(String code, String reference);
}
