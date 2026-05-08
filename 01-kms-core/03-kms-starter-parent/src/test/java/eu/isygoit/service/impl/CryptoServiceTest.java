package eu.isygoit.service.impl;

import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.repository.DigesterConfigRepository;
import eu.isygoit.repository.PEBConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CryptoServiceTest {

    @Mock
    private PEBConfigRepository pebConfigRepository;

    @Mock
    private DigesterConfigRepository digesterConfigRepository;

    @InjectMocks
    private CryptoService cryptoService;

    @Test
    void testGenerateKeyMaterial_AES() {
        byte[] key = cryptoService.generateKeyMaterial(IEnumKeySpec.Types.AES_256);
        assertNotNull(key);
        assertEquals(32, key.length); // 256 bits = 32 bytes
    }

    @Test
    void testGenerateKeyMaterial_RSA() {
        byte[] key = cryptoService.generateKeyMaterial(IEnumKeySpec.Types.RSA_2048);
        assertNotNull(key);
        assertTrue(key.length > 0);
    }

    @Test
    void testEncryptDecryptData_AES_GCM() {
        byte[] plaintext = "hello world".getBytes();
        byte[] keyMaterial = new byte[32]; // 256-bit key
        Map<String, String> context = Map.of("key", "value");

        byte[] encrypted = cryptoService.encryptData(plaintext, keyMaterial, context);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 12); // IV (12) + Ciphertext

        byte[] decrypted = cryptoService.decryptData("default", encrypted, keyMaterial, context);
        assertNotNull(decrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void testSignVerify_RSA() {
        byte[] message = "hello world".getBytes();
        // Generate a real RSA key pair for testing
        java.security.KeyPairGenerator keyGen;
        try {
            keyGen = java.security.KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            java.security.KeyPair pair = keyGen.generateKeyPair();
            byte[] privateKey = pair.getPrivate().getEncoded();
            byte[] publicKey = pair.getPublic().getEncoded();

            byte[] signature = cryptoService.signData(message, privateKey, "SHA256withRSA");
            assertNotNull(signature);

            boolean verified = cryptoService.verifySignature(message, signature, publicKey, "SHA256withRSA");
            assertTrue(verified);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
