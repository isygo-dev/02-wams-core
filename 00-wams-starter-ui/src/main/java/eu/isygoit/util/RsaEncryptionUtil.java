package eu.isygoit.util;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaEncryptionUtil {

    private RsaEncryptionUtil() {
    }

    public static String encryptWithPublicKey(String publicKeyBase64, String plaintextBase64) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] plaintext = Base64.getDecoder().decode(plaintextBase64);
        byte[] ciphertext = cipher.doFinal(plaintext);
        return Base64.getEncoder().encodeToString(ciphertext);
    }
}