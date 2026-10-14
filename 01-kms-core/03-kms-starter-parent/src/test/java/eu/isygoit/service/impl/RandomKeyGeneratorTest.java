package eu.isygoit.service.impl;

import eu.isygoit.enums.IEnumCharSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B3: the shared singleton generator must be thread-safe (no shared buffer) and honour length and char set.
 */
class RandomKeyGeneratorTest {

    private final RandomKeyGenerator generator = new RandomKeyGenerator();

    @Test
    void honoursLengthAndNumericCharSet() {
        String code = generator.nextGuid(6, IEnumCharSet.Types.NUMERIC);

        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
    }

    @Test
    void concurrentCallsWithDifferentLengthsNeverInterfere() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<String>> futures = new ArrayList<>();
            List<Integer> lengths = new ArrayList<>();
            for (int i = 0; i < 4000; i++) {
                int length = 4 + (i % 9); // 4..12
                lengths.add(length);
                Callable<String> task = () -> generator.nextGuid(length, IEnumCharSet.Types.NUMERIC);
                futures.add(pool.submit(task));
            }
            for (int i = 0; i < futures.size(); i++) {
                String value = futures.get(i).get();
                assertEquals(lengths.get(i).intValue(), value.length());
                assertTrue(value.chars().allMatch(Character::isDigit), "unexpected character in " + value);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void explicitLengthDoesNotChangeTheDefaultLength() {
        generator.setBufferLength(10);

        generator.nextGuid(6, IEnumCharSet.Types.NUMERIC);

        assertEquals(10, generator.nextGuid(IEnumCharSet.Types.NUMERIC).length());
    }

    @Test
    void successiveCodesDiffer() {
        String first = generator.nextGuid(12, IEnumCharSet.Types.ALL);
        String second = generator.nextGuid(12, IEnumCharSet.Types.ALL);

        assertTrue(!first.equals(second));
    }
}
