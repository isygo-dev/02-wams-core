package eu.isygoit.simulation;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExternalKeyProxyInstance {
    private final Long storeId;
    private final String endpoint;
    private final String path;
    private boolean connected;
    private String sessionId;
    private final ConcurrentHashMap<String, byte[]> remoteKeys = new ConcurrentHashMap<>();

    public ExternalKeyProxyInstance(Long storeId, String endpoint, String path) {
        this.storeId = storeId;
        this.endpoint = endpoint;
        this.path = path;
    }

    public boolean connect(String authCredentialHash) {
        if (authCredentialHash != null && !authCredentialHash.isEmpty()) {
            this.connected = true;
            this.sessionId = UUID.randomUUID().toString();
            return true;
        }
        return false;
    }

    public void disconnect() {
        this.connected = false;
        this.sessionId = null;
    }

    public boolean isConnected() {
        return connected;
    }

    public SecretKey generateKey(String algorithm, int keySize, String keyId) throws Exception {
        // Simulate remote key generation: store a random AES key locally (as simulation)
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        kg.init(keySize);
        SecretKey key = kg.generateKey();
        remoteKeys.put(keyId, key.getEncoded());
        return key;
    }

    public byte[] encrypt(String keyId, byte[] plaintext, Map<String, String> context) throws Exception {
        byte[] keyBytes = remoteKeys.get(keyId);
        if (keyBytes == null) throw new IllegalArgumentException("Key not found");
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    public byte[] decrypt(String keyId, byte[] ciphertextWithIv, Map<String, String> context) throws Exception {
        byte[] keyBytes = remoteKeys.get(keyId);
        if (keyBytes == null) throw new IllegalArgumentException("Key not found");
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        byte[] iv = Arrays.copyOfRange(ciphertextWithIv, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(ciphertextWithIv, 12, ciphertextWithIv.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }

    public byte[] sign(String keyId, byte[] message, String algorithm) throws Exception {
        // For simulation, use HMAC-SHA256
        byte[] keyBytes = remoteKeys.get(keyId);
        if (keyBytes == null) throw new IllegalArgumentException("Key not found");
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(message);
    }

    public boolean verify(String keyId, byte[] message, byte[] signature, String algorithm) throws Exception {
        byte[] computed = sign(keyId, message, algorithm);
        return MessageDigest.isEqual(computed, signature);
    }

    public KmsDtos.GenerateDataKeyResponse generateDataKey(String keyId, String keySpec, Map<String, String> context) throws Exception {
        int size = "AES_128".equals(keySpec) ? 128 : 256;
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(size);
        SecretKey dataKey = kg.generateKey();
        byte[] encrypted = encrypt(keyId, dataKey.getEncoded(), context);
        return KmsDtos.GenerateDataKeyResponse.builder()
                .ciphertextBlob(Base64.getEncoder().encodeToString(encrypted))
                .plaintext(Base64.getEncoder().encodeToString(dataKey.getEncoded()))
                .keyId(keyId)
                .encryptionAlgorithmSpec("SYMMETRIC_DEFAULT")
                .build();
    }

    public KmsDtos.GenerateDataKeyPairResponse generateDataKeyPair(String keyId, IEnumKeySpec.Types spec, Map<String, String> context) throws Exception {
        KeyPairGenerator kpg;
        if (spec == IEnumKeySpec.Types.RSA_2048) {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
        } else if (spec == IEnumKeySpec.Types.ECC_NIST_P256) {
            kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
        } else {
            throw new IllegalArgumentException("Unsupported key pair spec");
        }
        KeyPair kp = kpg.generateKeyPair();
        byte[] privateKeyEncrypted = encrypt(keyId, kp.getPrivate().getEncoded(), context);
        return KmsDtos.GenerateDataKeyPairResponse.builder()
                .publicKey(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()))
                .privateKeyCiphertextBlob(Base64.getEncoder().encodeToString(privateKeyEncrypted))
                .keyId(keyId)
                .keyPairSpec(spec)
                .encryptionAlgorithmSpec("SYMMETRIC_DEFAULT")
                .build();
    }

    public void deleteKey(String keyId) {
        remoteKeys.remove(keyId);
    }
}
