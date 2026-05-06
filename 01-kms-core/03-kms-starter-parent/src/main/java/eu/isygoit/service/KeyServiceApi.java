package eu.isygoit.service;

import eu.isygoit.enums.IEnumCharSet;
import org.springframework.http.ResponseEntity;

/**
 * The interface Key service api.
 * Legacy interface for random key generation APIs.
 */
public interface KeyServiceApi {

    /**
     * New random key response entity.
     *
     * @param length      the length
     * @param charSetType the char set type
     * @return the response entity
     */
    ResponseEntity<String> newRandomKey(Integer length, IEnumCharSet.Types charSetType);

    /**
     * Renew random key response entity.
     *
     * @param tenant      the tenant
     * @param keyName     the key name
     * @param length      the length
     * @param charSetType the char set type
     * @return the response entity
     */
    ResponseEntity<String> renewRandomKey(String tenant, String keyName, Integer length, IEnumCharSet.Types charSetType);

    /**
     * Get random key response entity.
     *
     * @param tenant  the tenant
     * @param keyName the key name
     * @return the response entity
     */
    ResponseEntity<String> getRandomKey(String tenant, String keyName);
}

