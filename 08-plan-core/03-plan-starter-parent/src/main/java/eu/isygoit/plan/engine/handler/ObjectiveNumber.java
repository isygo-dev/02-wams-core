package eu.isygoit.plan.engine.handler;

import eu.isygoit.plan.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

final class ObjectiveNumber {
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private ObjectiveNumber() {
    }

    static long roundedLong(Object value, String description) {
        if (!(value instanceof Number number)) {
            throw invalid(description);
        }

        BigDecimal decimal;
        if (number instanceof BigDecimal bigDecimal) {
            decimal = bigDecimal;
        } else if (number instanceof BigInteger bigInteger) {
            decimal = new BigDecimal(bigInteger);
        } else if (number instanceof Byte || number instanceof Short
                || number instanceof Integer || number instanceof Long) {
            decimal = BigDecimal.valueOf(number.longValue());
        } else {
            double doubleValue = number.doubleValue();
            if (!Double.isFinite(doubleValue)) {
                throw invalid(description);
            }
            decimal = BigDecimal.valueOf(doubleValue);
        }

        try {
            return decimal.add(HALF).setScale(0, RoundingMode.FLOOR).longValueExact();
        } catch (ArithmeticException exception) {
            throw invalid(description);
        }
    }

    static long negatedRoundedLong(Object value, String description) {
        try {
            return Math.negateExact(roundedLong(value, description));
        } catch (ArithmeticException exception) {
            throw invalid(description);
        }
    }

    private static InvalidRequestException invalid(String description) {
        return new InvalidRequestException(
                "INVALID_OBJECTIVE_DATA",
                description + " must be a finite number representable as a 64-bit integer");
    }
}
