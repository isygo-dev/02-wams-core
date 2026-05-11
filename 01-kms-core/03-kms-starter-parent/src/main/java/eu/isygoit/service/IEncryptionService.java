package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos;

/**
 * The interface Encryption service.
 */
public interface IEncryptionService {

    /**
     * Encrypt.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the encrypt response dto
     */
    KmsDtos.EncryptResponse encrypt(
            String tenant,
            KmsDtos.EncryptRequest request);

    /**
     * Decrypt.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the decrypt response dto
     */
    public KmsDtos.DecryptResponse decrypt(
            String tenant,
            KmsDtos.DecryptRequest request);

    /**
     * Re-encrypt.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the encrypt response dto
     */
    KmsDtos.ReEncryptResponse reEncrypt(
            String tenant,
            KmsDtos.ReEncryptRequest request);
}

