package eu.isygoit.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.isygoit.exception.ParamsInvalidException;

import java.lang.reflect.RecordComponent;
import java.util.Map;

/**
 * Strict binder of the free {@code params} map of a {@code Constraint} / {@code Objective} to the typed params
 * record of its family (package {@code eu.isygoit.dto.request.params}).
 * <p>
 * Strictness: unknown properties fail, scalars are not coerced ("5" is not an int, 5.5 is not an int, numbers are
 * not enums), an explicit {@code null} for a primitive fails, and a MISSING primitive component (for example
 * {@code MaxConsecutiveParams.max}) fails. Missing boxed/object components are allowed (they are optional and
 * receive the record defaults). All failures are reported as {@link ParamsInvalidException} (code
 * {@code PARAMS_INVALID}) carrying the path of the offending property.
 * <pre>{@code
 * MinRestParams p = ParamsBinder.bind(constraint.params(), MinRestParams.class, "constraints[2].params");
 * }</pre>
 * Families without parameters declare {@code Void.class}: binding then only checks that the map is empty and
 * returns {@code null}.
 */
public final class ParamsBinder {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private ParamsBinder() {
    }

    /**
     * Binds {@code params} to {@code type}; error paths are relative to the params object.
     *
     * @param params the raw params map (null = empty)
     * @param type   the typed params record (or {@code Void.class})
     * @param <P>    the params type
     * @return the bound record, or {@code null} for {@code Void.class}
     * @throws ParamsInvalidException when the binding is not strict-valid
     */
    public static <P> P bind(Map<String, Object> params, Class<P> type) {
        return bind(params, type, "");
    }

    /**
     * Binds {@code params} to {@code type}; the error path is {@code pathPrefix + "." + propertyPath}.
     *
     * @param params     the raw params map (null = empty)
     * @param type       the typed params record (or {@code Void.class})
     * @param pathPrefix location of the params object in the request, for example {@code constraints[2].params}
     *                   (may be null or empty)
     * @param <P>        the params type
     * @return the bound record, or {@code null} for {@code Void.class}
     * @throws ParamsInvalidException when the binding is not strict-valid
     */
    public static <P> P bind(Map<String, Object> params, Class<P> type, String pathPrefix) {
        String prefix = pathPrefix == null ? "" : pathPrefix;
        Map<String, Object> source = params == null ? Map.of() : params;
        if (type == Void.class || type == void.class) {
            if (!source.isEmpty()) {
                String first = String.valueOf(source.keySet().iterator().next());
                throw new ParamsInvalidException(join(prefix, first),
                        "unknown property '" + first + "': this family takes no params", null);
            }
            return null;
        }
        try {
            JsonNode tree = MAPPER.valueToTree(source);
            checkRequiredPrimitives(tree, type, prefix);
            return MAPPER.treeToValue(tree, type);
        } catch (JsonMappingException e) {
            throw new ParamsInvalidException(join(prefix, relativePath(e)), describe(e), e);
        } catch (JsonProcessingException e) {
            throw new ParamsInvalidException(prefix, describe(e), e);
        } catch (IllegalArgumentException e) {
            throw new ParamsInvalidException(prefix, "params cannot be converted: " + e.getMessage(), e);
        }
    }

    private static void checkRequiredPrimitives(JsonNode tree, Class<?> type, String prefix) {
        if (!type.isRecord()) {
            return;
        }
        if (!(tree instanceof ObjectNode)) {
            return;
        }
        for (RecordComponent component : type.getRecordComponents()) {
            if (component.getType().isPrimitive() && !tree.has(component.getName())) {
                throw new ParamsInvalidException(join(prefix, component.getName()),
                        "missing required property '" + component.getName() + "'", null);
            }
        }
    }

    private static String relativePath(JsonMappingException e) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() != null) {
                if (path.length() > 0) {
                    path.append('.');
                }
                path.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String describe(JsonProcessingException e) {
        String message = e.getOriginalMessage();
        return message == null ? e.getClass().getSimpleName() : message;
    }

    private static String join(String prefix, String path) {
        if (prefix.isEmpty()) {
            return path;
        }
        return path == null || path.isEmpty() ? prefix : prefix + "." + path;
    }
}
