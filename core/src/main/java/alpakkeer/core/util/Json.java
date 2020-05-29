package alpakkeer.core.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

/**
 * A util class to work with JSON.
 */
@AllArgsConstructor(staticName = "apply")
public final class Json {

    private final ObjectMapper om;

    /**
     * Deserializes a JSON to a java object with generic type parameters (and fields).
     *
     * @param json           The JSON to de-serialize
     * @param baseType       The base type of the class to be de-serialized
     * @param parameterTypes The parameter types of the base class
     * @param <T>            The base type
     * @return The de-serialized object instance
     */
    public <T> T deserializeGenericClassFromJson(String json, Class<T> baseType, Class<?>... parameterTypes) {
        return Operators.suppressExceptions(() -> {
            JavaType genericType = om.getTypeFactory().constructParametricType(baseType, parameterTypes);
            return om.readValue(json, genericType);
        });
    }

    /**
     * Deserializes a JSON to a java object with generic type parameters (and fields). The generic types are not known by default
     * and are stored in fields in the json at root level.
     *
     * @param json      The JSON to-deserialize
     * @param baseType  The base type of the class to be de-serialized
     * @param typeHints The names of the fields at the root level of the JSON where type names of parameter types are stored
     * @param <T>       The base type
     * @return The de-serialized object instance
     */
    public <T> T deserializeDynamicGenericClassFromJson(String json, Class<T> baseType, String... typeHints) {
        return Operators.suppressExceptions(() -> {
            JsonNode node = om.readTree(json);

            Class<?>[] parameterTypes = new Class<?>[typeHints.length];
            for (int i = 0; i < typeHints.length; i++) {
                String typeName = node.get(typeHints[i]).asText();
                parameterTypes[i] = Class.forName(typeName);
            }

            return deserializeGenericClassFromJson(json, baseType, parameterTypes);
        });
    }

    /**
     * Deserializes a JSON to a java object with generic type parameters (and fields). The generic types are not known by default
     * and are stored in fields in the json at root level.
     *
     * This class is a generalization which assumes that there is just one generic parameter and the type hint is stored
     * in field "type".
     *
     * @param json      The JSON to-deserialize
     * @param baseType  The base type of the class to be de-serialized
     * @param <T>       The base type
     * @return The de-serialized object instance
     */
    public <T> T deserializeDynamicGenericClassFromJson(String json, Class<T> baseType) {
        return deserializeDynamicGenericClassFromJson(json, baseType, "type");
    }

}
