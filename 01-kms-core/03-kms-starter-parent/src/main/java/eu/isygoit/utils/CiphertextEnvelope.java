package eu.isygoit.utils;

import java.nio.charset.StandardCharsets;

// CiphertextEnvelope.java (new class)
public class CiphertextEnvelope {
    private static final String VERSION_PREFIX = "KMSv1:"; // simple marker

    public static byte[] wrap(String versionId, byte[] ciphertext) {
        byte[] versionBytes = versionId.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[versionBytes.length + 1 + ciphertext.length];
        System.arraycopy(versionBytes, 0, result, 0, versionBytes.length);
        result[versionBytes.length] = 0; // separator
        System.arraycopy(ciphertext, 0, result, versionBytes.length + 1, ciphertext.length);
        return result;
    }

    public static String unwrapVersionId(byte[] wrapped) {
        // Find the first zero byte
        for (int i = 0; i < wrapped.length; i++) {
            if (wrapped[i] == 0) {
                return new String(wrapped, 0, i, StandardCharsets.UTF_8);
            }
        }
        return null; // no version -> legacy
    }

    public static byte[] unwrapCiphertext(byte[] wrapped) {
        for (int i = 0; i < wrapped.length; i++) {
            if (wrapped[i] == 0) {
                byte[] ciphertext = new byte[wrapped.length - i - 1];
                System.arraycopy(wrapped, i + 1, ciphertext, 0, ciphertext.length);
                return ciphertext;
            }
        }
        return wrapped; // legacy
    }
}