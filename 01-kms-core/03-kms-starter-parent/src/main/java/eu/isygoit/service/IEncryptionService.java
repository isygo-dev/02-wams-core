package eu.isygoit.service;

import eu.isygoit.dto.request.DecryptRequestDto;
import eu.isygoit.dto.request.EncryptRequestDto;
import eu.isygoit.dto.request.ReencryptRequestDto;
import eu.isygoit.dto.response.DecryptResponseDto;
import eu.isygoit.dto.response.EncryptResponseDto;

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
    EncryptResponseDto encrypt(String tenant, EncryptRequestDto request);

    /**
     * Decrypt.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the decrypt response dto
     */
    DecryptResponseDto decrypt(String tenant, DecryptRequestDto request);

    /**
     * Re-encrypt.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the encrypt response dto
     */
    EncryptResponseDto reencrypt(String tenant, ReencryptRequestDto request);
}

