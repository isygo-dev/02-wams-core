package eu.isygoit.enums;

import java.util.ArrayList;
import java.util.List;

public interface IEnumKeySpec {

    int STR_ENUM_SIZE = 14;

    enum Types implements IEnum {

        SYMMETRIC_DEFAULT("SYMMETRIC_DEFAULT", "AES", 256, false, IEnumKeyUsage.Types.ENCRYPT_DECRYPT),
        RSA_2048("RSA_2048", "RSA", 2048, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY),
        RSA_3072("RSA_3072", "RSA", 3072, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY),
        RSA_4096("RSA_4096", "RSA", 4096, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY),
        ECC_NIST_P256("ECC_NIST_P256", "EC", 256, true, IEnumKeyUsage.Types.SIGN_VERIFY),
        ECC_NIST_P384("ECC_NIST_P384", "EC", 384, true, IEnumKeyUsage.Types.SIGN_VERIFY),
        ECC_NIST_P521("ECC_NIST_P521", "EC", 521, true, IEnumKeyUsage.Types.SIGN_VERIFY),
        ECC_SECG_P256K1("ECC_SECG_P256K1", "EC", 256, true, IEnumKeyUsage.Types.SIGN_VERIFY),
        HMAC_224("HMAC_224", "HmacSHA224", 224, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),
        HMAC_256("HMAC_256", "HmacSHA256", 256, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),
        HMAC_384("HMAC_384", "HmacSHA384", 384, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),
        HMAC_512("HMAC_512", "HmacSHA512", 512, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),
        SM2("SM2", "SM2", 256, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY);

        private final String wamsName;
        private final String javaAlgorithm;
        private final int keySizeBits;
        private final boolean asymmetric;
        private final List<IEnumKeyUsage.Types> allowedUsages;

        Types(String wamsName, String javaAlgorithm, int keySizeBits, boolean asymmetric, IEnumKeyUsage.Types... allowedUsages) {
            this.wamsName = wamsName;
            this.javaAlgorithm = javaAlgorithm;
            this.keySizeBits = keySizeBits;
            this.asymmetric = asymmetric;
            this.allowedUsages = new ArrayList<>();
            for (IEnumKeyUsage.Types usage : allowedUsages) {
                this.allowedUsages.add(usage);
            }
        }

        @Override
        public String meaning() {
            return wamsName;
        }

        public String getJavaAlgorithm() {
            return javaAlgorithm;
        }

        public int getKeySizeBits() {
            return keySizeBits;
        }

        public boolean isAsymmetric() {
            return asymmetric;
        }

        public List<IEnumKeyUsage.Types> allowedUsages() {
            return allowedUsages;
        }

        public String getCurveName() {
            switch (this) {
                case ECC_NIST_P256:
                    return "secp256r1";
                case ECC_NIST_P384:
                    return "secp384r1";
                case ECC_NIST_P521:
                    return "secp521r1";
                case ECC_SECG_P256K1:
                    return "secp256k1";
                case SM2:
                    return "sm2p256v1";
                default:
                    return null;
            }
        }

        public int getKeySizeBytes() {
            return keySizeBits / 8;
        }
    }
}