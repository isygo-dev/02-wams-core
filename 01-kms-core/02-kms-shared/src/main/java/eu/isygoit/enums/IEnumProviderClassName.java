package eu.isygoit.enums;

public interface IEnumProviderClassName {

    int STR_ENUM_SIZE = 100;

    enum Types implements IEnum {

        BOUNCY_CASTLE("Bouncy Castle", "BC", "org.bouncycastle.jce.provider.BouncyCastleProvider"),
        SUN_JCE("Sun JCE", "SunJCE", "com.sun.crypto.provider.SunJCE"),
        SUN_PKCS11("Sun PKCS11", "SunPKCS11", "sun.security.pkcs11.SunPKCS11"),
        IBM_JCE("IBM JCE", "IBMJCE", "com.ibm.crypto.provider.IBMJCE"),
        CONSCRYPT("Conscrypt", "Conscrypt", "org.conscrypt.ConscryptProvider");

        private final String meaning;
        private final String providerName;
        private final String classPath;

        Types(String meaning, String providerName, String classPath) {
            this.meaning = meaning;
            this.providerName = providerName;
            this.classPath = classPath;
        }

        @Override
        public String meaning() {
            return meaning;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getClassPath() {
            return classPath;
        }
    }
}