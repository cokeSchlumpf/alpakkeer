package alpakkeer.core.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ObjectMapperFactory {

    private ObjectMapperFactory() {

    }

    public static ObjectMapperFactory apply() {
        return new ObjectMapperFactory();
    }

    public ObjectMapper create(boolean pretty) {
        ObjectMapper om = new ObjectMapper();
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.registerModule(new JavaTimeModule());
        om.registerModule(new Jdk8Module());

        om.getSerializationConfig()
          .getDefaultVisibilityChecker()
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withCreatorVisibility(JsonAutoDetect.Visibility.ANY);

        om.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        om.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        SimpleModule module = new SimpleModule();
        om.registerModule(module);

        if (pretty) {
            om.enable(SerializationFeature.INDENT_OUTPUT);
        }

        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return om;
    }

    public ObjectMapper create() {
        return create(false);
    }

}
