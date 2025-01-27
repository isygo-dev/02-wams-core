package eu.isygoit.service;

import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.IncrementalConfigNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.RandomKey;

import java.util.Optional;

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
     * @param domain     the domain
     * @param entityName the entity name
     * @param attribute  the attribute
     * @return the incremental key
     * @throws IncrementalConfigNotFoundException the incremental config not found exception
     */
    String getIncrementalKey(String domain, String entityName, String attribute) throws IncrementalConfigNotFoundException;

    /**
     * Create or update key by name random key.
     *
     * @param domain the domain
     * @param name   the name
     * @param value  the value
     * @return the random key
     */
    RandomKey createOrUpdateKeyByName(String domain, String name, String value);

    /**
     * Gets key by name.
     *
     * @param domain the domain
     * @param name   the name
     * @return the key by name
     */
    Optional<RandomKey> getKeyByName(String domain, String name);
}
