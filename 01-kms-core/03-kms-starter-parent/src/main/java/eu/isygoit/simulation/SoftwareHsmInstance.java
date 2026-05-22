package eu.isygoit.simulation;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoftwareHsmInstance {
    private final Long storeId;
    private final String name;
    private final ConcurrentHashMap<String, SecretKey> symmetricKeys;
    private final ConcurrentHashMap<String, KeyPair> asymmetricKeys;
    private boolean connected;
    private LocalDateTime connectedAt;

    public SoftwareHsmInstance(Long storeId, String name) {
        this.storeId = storeId;
        this.name = name;
        this.symmetricKeys = new ConcurrentHashMap<>();
        this.asymmetricKeys = new ConcurrentHashMap<>();
        this.connected = false;
    }

    public synchronized boolean connect(String passwordHash) {
        // In real HSM, validate password; here we accept any non-empty hash
        if (passwordHash != null && !passwordHash.isEmpty()) {
            this.connected = true;
            this.connectedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public synchronized void disconnect() {
        this.connected = false;
        this.connectedAt = null;
    }

    public boolean isConnected() {
        return connected;
    }

    public SecretKey generateKey(String algorithm, int keySize, String keyId) throws Exception {
        if (!connected) throw new IllegalStateException("HSM not connected");
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        kg.init(keySize);
        SecretKey key = kg.generateKey();
        symmetricKeys.put(keyId, key);
        return key;
    }

    public byte[] encrypt(String keyId, byte[] plaintext, Map<String, String> context) throws Exception {
        SecretKey key = symmetricKeys.get(keyId);
        if (key == null) throw new IllegalArgumentException("Key not found: " + keyId);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(plaintext);
        // Prepend IV
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    public byte[] decrypt(String keyId, byte[] ciphertextWithIv, Map<String, String> context) throws Exception {
        SecretKey key = symmetricKeys.get(keyId);
        if (key == null) throw new IllegalArgumentException("Key not found: " + keyId);
        byte[] iv = Arrays.copyOfRange(ciphertextWithIv, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(ciphertextWithIv, 12, ciphertextWithIv.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }

    public byte[] sign(String keyId, byte[] message, String algorithm) throws Exception {
        KeyPair kp = asymmetricKeys.get(keyId);
        if (kp == null) throw new IllegalArgumentException("Private key not found");
        Signature sig = Signature.getInstance(algorithm);
        sig.initSign(kp.getPrivate());
        sig.update(message);
        return sig.sign();
    }

    public boolean verify(String keyId, byte[] message, byte[] signature, String algorithm) throws Exception {
        KeyPair kp = asymmetricKeys.get(keyId);
        if (kp == null) throw new IllegalArgumentException("Public key not found");
        Signature sig = Signature.getInstance(algorithm);
        sig.initVerify(kp.getPublic());
        sig.update(message);
        return sig.verify(signature);
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
        symmetricKeys.remove(keyId);
        asymmetricKeys.remove(keyId);
    }
}
