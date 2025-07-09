package eu.isygoit.service;

import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.IncrementalConfigNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.RandomKey;

/**
 * The interface Key service.
 */
public interface IKeyService {

    /**
     * Gets random key.
     *
     * @param length      the length
     * @param charSetType the char set type
     * @return the random key
     */
    String getRandomKey(int length, IEnumCharSet.Types charSetType);

    /**
     * Gets random key.
     *
     * @param length      the length
     * @param charSetType the char set type
     * @param pattern     the pattern
     * @return the random key
     */
    String getRandomKey(int length, IEnumCharSet.Types charSetType, String pattern);

    /**
     * Subscribe incremental key generator.
     *
     * @param appNextCode the app next code
     */
    void subscribeIncrementalKeyGenerator(AppNextCode appNextCode);

    /**
     * Gets incremental key.
     *
     * @param tenant     the tenant
     * @param entityName the entity name
     * @param attribute  the attribute
     * @return the incremental key
     * @throws IncrementalConfigNotFoundException the incremental config not found exception
     */
    String getIncrementalKey(String tenant, String entityName, String attribute) throws IncrementalConfigNotFoundException;

    /**
     * Create or update key by name random key.
     *
     * @param tenant the tenant
     * @param name   the name
     * @param value  the value
     * @return the random key
     */
    RandomKey createOrUpdateKeyByName(String tenant, String name, String value);

    /**
     * Gets key by name.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the key by name
     */
    RandomKey getKeyByName(String tenant, String name);
}
