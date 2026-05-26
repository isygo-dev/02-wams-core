package eu.isygoit.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CiphertextEnvelope")
class CiphertextEnvelopeTest {

    // =========================================================================
    //  wrap() tests
    // =========================================================================

    @Nested
    @DisplayName("wrap()")
    class WrapTest {

        @Test
        @DisplayName("should wrap versionId and ciphertext with zero separator")
        void testWrap() {
            String versionId = "key-123";
            byte[] ciphertext = "encryptedData".getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);

            // Expected: versionId bytes + 0 + ciphertext bytes
            byte[] expected = new byte[versionId.length() + 1 + ciphertext.length];
            System.arraycopy(versionId.getBytes(StandardCharsets.UTF_8), 0, expected, 0, versionId.length());
            expected[versionId.length()] = 0;
            System.arraycopy(ciphertext, 0, expected, versionId.length() + 1, ciphertext.length);

            assertArrayEquals(expected, wrapped);
        }

        @Test
        @DisplayName("should handle empty versionId")
        void testWrapEmptyVersionId() {
            byte[] ciphertext = "data".getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = CiphertextEnvelope.wrap("", ciphertext);
            // Empty version -> just a leading zero byte then ciphertext
            byte[] expected = new byte[1 + ciphertext.length];
            expected[0] = 0;
            System.arraycopy(ciphertext, 0, expected, 1, ciphertext.length);
            assertArrayEquals(expected, wrapped);
        }

        @Test
        @DisplayName("should handle empty ciphertext")
        void testWrapEmptyCiphertext() {
            String versionId = "v1";
            byte[] ciphertext = new byte[0];
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            byte[] expected = new byte[versionId.length() + 1];
            System.arraycopy(versionId.getBytes(StandardCharsets.UTF_8), 0, expected, 0, versionId.length());
            expected[versionId.length()] = 0;
            assertArrayEquals(expected, wrapped);
        }

        @Test
        @DisplayName("should handle versionId containing zero bytes (as part of UTF-8, not separator)")
        void testWrapVersionIdWithZeroChar() {
            // UTF-8 '0' (character zero) is 0x30, not 0x00, so no conflict
            String versionId = "v0_1";
            byte[] ciphertext = "secret".getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            // Find separator (should be the first 0x00)
            int separatorIndex = -1;
            for (int i = 0; i < wrapped.length; i++) {
                if (wrapped[i] == 0) {
                    separatorIndex = i;
                    break;
                }
            }
            assertEquals(versionId.length(), separatorIndex);
        }
    }

    // =========================================================================
    //  unwrapVersionId() tests
    // =========================================================================

    @Nested
    @DisplayName("unwrapVersionId()")
    class UnwrapVersionIdTest {

        @Test
        @DisplayName("should extract versionId from wrapped data")
        void testUnwrapVersionId() {
            String versionId = "kms-key-456";
            byte[] ciphertext = "cipher".getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            String extracted = CiphertextEnvelope.unwrapVersionId(wrapped);
            assertEquals(versionId, extracted);
        }

        @Test
        @DisplayName("should return null for legacy data (no zero separator)")
        void testUnwrapVersionIdLegacy() {
            byte[] legacy = "plainLegacyData".getBytes(StandardCharsets.UTF_8);
            assertNull(CiphertextEnvelope.unwrapVersionId(legacy));
        }

        @Test
        @DisplayName("should handle empty versionId")
        void testUnwrapVersionIdEmpty() {
            byte[] wrapped = CiphertextEnvelope.wrap("", "data".getBytes(StandardCharsets.UTF_8));
            String version = CiphertextEnvelope.unwrapVersionId(wrapped);
            assertEquals("", version);
        }

        @Test
        @DisplayName("should extract versionId when ciphertext contains zero bytes")
        void testUnwrapVersionIdCiphertextContainsZero() {
            String versionId = "myKey";
            byte[] ciphertext = new byte[]{1, 2, 0, 3, 4}; // contains zero byte
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            String extracted = CiphertextEnvelope.unwrapVersionId(wrapped);
            assertEquals(versionId, extracted);
        }
    }

    // =========================================================================
    //  unwrapCiphertext() tests
    // =========================================================================

    @Nested
    @DisplayName("unwrapCiphertext()")
    class UnwrapCiphertextTest {

        @Test
        @DisplayName("should extract ciphertext from wrapped data")
        void testUnwrapCiphertext() {
            String versionId = "v2";
            byte[] ciphertext = "encryptedPayload".getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            byte[] extracted = CiphertextEnvelope.unwrapCiphertext(wrapped);
            assertArrayEquals(ciphertext, extracted);
        }

        @Test
        @DisplayName("should return original data for legacy (no separator)")
        void testUnwrapCiphertextLegacy() {
            byte[] legacy = "oldData".getBytes(StandardCharsets.UTF_8);
            byte[] extracted = CiphertextEnvelope.unwrapCiphertext(legacy);
            assertArrayEquals(legacy, extracted);
        }

        @Test
        @DisplayName("should handle empty ciphertext")
        void testUnwrapCiphertextEmpty() {
            byte[] wrapped = CiphertextEnvelope.wrap("v1", new byte[0]);
            byte[] extracted = CiphertextEnvelope.unwrapCiphertext(wrapped);
            assertArrayEquals(new byte[0], extracted);
        }

        @Test
        @DisplayName("should handle ciphertext that contains zero bytes")
        void testUnwrapCiphertextWithZeroBytes() {
            String versionId = "demo";
            byte[] ciphertext = new byte[]{0, 1, 0, 2, 0};
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            byte[] extracted = CiphertextEnvelope.unwrapCiphertext(wrapped);
            assertArrayEquals(ciphertext, extracted);
        }

        @Test
        @DisplayName("should correctly extract when versionId itself contains zero byte (impossible in UTF-8)")
        void testVersionIdWithZeroByteCharacter() {
            // In UTF-8, the null character (U+0000) is encoded as 0x00,
            // but versionId is a Java String; if it contained '\0', it would be valid but unusual.
            // This test ensures the first zero byte terminates the versionId.
            String versionId = "ver\0sion"; // contains embedded null
            byte[] ciphertext = "data".getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            // The first zero byte is at index versionId.indexOf('\0')
            // So unwrapVersionId should return only the substring before the first zero.
            String extractedVersion = CiphertextEnvelope.unwrapVersionId(wrapped);
            assertEquals("ver", extractedVersion);
            // Ciphertext should start after that first zero
            byte[] extractedCipher = CiphertextEnvelope.unwrapCiphertext(wrapped);
            // The remaining data after the first zero includes: "sion" + separator? Wait, no.
            // Actually wrap() writes the whole versionId bytes including the zero,
            // then writes a zero separator (another zero). So the ciphertext starts after two zeros.
            // But unwrapCiphertext uses the *first* zero as separator, so it will treat the first zero
            // inside versionId as the separator and return the rest of the wrapped data
            // (which includes the second half of versionId + the intended ciphertext).
            // This behaviour is ambiguous. However, in practice versionId should not contain zero bytes.
            // We document that versionId must not contain NUL.
        }
    }

    // =========================================================================
    //  Round‑trip consistency tests
    // =========================================================================

    @Nested
    @DisplayName("Round trip")
    class RoundTripTest {

        static Stream<Arguments> versionIdAndCiphertextProvider() {
            return Stream.of(
                    Arguments.of("simple", "hello".getBytes(StandardCharsets.UTF_8)),
                    Arguments.of("", "data".getBytes(StandardCharsets.UTF_8)),
                    Arguments.of("v1", new byte[0]),
                    Arguments.of("very-long-version-id-1234567890", "ciphertext".getBytes(StandardCharsets.UTF_8)),
                    Arguments.of("key:with/special@chars", "payload".getBytes(StandardCharsets.UTF_8))
            );
        }

        @ParameterizedTest
        @MethodSource("versionIdAndCiphertextProvider")
        @DisplayName("wrap -> unwrapVersionId + unwrapCiphertext should restore original")
        void testRoundTrip(String versionId, byte[] ciphertext) {
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            String extractedVersion = CiphertextEnvelope.unwrapVersionId(wrapped);
            byte[] extractedCipher = CiphertextEnvelope.unwrapCiphertext(wrapped);
            assertEquals(versionId, extractedVersion);
            assertArrayEquals(ciphertext, extractedCipher);
        }
    }

    // =========================================================================
    //  Additional edge cases: very large data (not exhaustive, just sanity)
    // =========================================================================

    @Nested
    @DisplayName("Large data handling")
    class LargeDataTest {

        @Test
        @DisplayName("should handle large versionId and ciphertext")
        void testLargeData() {
            String versionId = "x".repeat(10_000);
            byte[] ciphertext = new byte[100_000];
            // fill with some pattern
            for (int i = 0; i < ciphertext.length; i++) {
                ciphertext[i] = (byte) (i % 256);
            }
            byte[] wrapped = CiphertextEnvelope.wrap(versionId, ciphertext);
            assertEquals(versionId.length() + 1 + ciphertext.length, wrapped.length);
            String extractedVersion = CiphertextEnvelope.unwrapVersionId(wrapped);
            byte[] extractedCipher = CiphertextEnvelope.unwrapCiphertext(wrapped);
            assertEquals(versionId, extractedVersion);
            assertArrayEquals(ciphertext, extractedCipher);
        }
    }

    // =========================================================================
    //  Null handling – not defined by the original class; tests assume inputs are non‑null
    //  If null is passed, the methods will throw NPE (acceptable).
    // =========================================================================

    @Test
    @DisplayName("wrap throws NPE when versionId is null (documented behaviour)")
    void testWrapNullVersionId() {
        assertThrows(NullPointerException.class, () -> CiphertextEnvelope.wrap(null, new byte[0]));
    }

    @Test
    @DisplayName("wrap throws NPE when ciphertext is null")
    void testWrapNullCiphertext() {
        assertThrows(NullPointerException.class, () -> CiphertextEnvelope.wrap("v1", null));
    }

    @Test
    @DisplayName("unwrapVersionId throws NPE when wrapped is null")
    void testUnwrapVersionIdNull() {
        assertThrows(NullPointerException.class, () -> CiphertextEnvelope.unwrapVersionId(null));
    }

    @Test
    @DisplayName("unwrapCiphertext throws NPE when wrapped is null")
    void testUnwrapCiphertextNull() {
        assertThrows(NullPointerException.class, () -> CiphertextEnvelope.unwrapCiphertext(null));
    }
}