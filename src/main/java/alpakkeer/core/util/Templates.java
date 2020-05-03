package alpakkeer.core.util;

import com.google.common.collect.Maps;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.resource.exceptions.ResourceNotFoundException;

import java.util.Map;

/**
 * Util class to work with templates.
 *
 * @author Michael Wellner (michael_wellner@rcomext.com).
 */
public final class Templates {

    /**
     * Do not create instances.
     */
    private Templates() {

    }

    /**
     * Renders a template with a map of values as model.
     *
     * @param template The template
     * @param model    The model
     * @return The rendered template
     */
    public static String render(JtwigTemplate template, Map<String, Object> model) {
        return template.render(createModel(model));
    }

    /**
     * Renders a template from a classpath resource with Jtwig.
     *
     * @param resourcePath The path of the resource
     * @return The rendered resource
     */
    public static String renderTemplateFromResources(String resourcePath) {
        return renderTemplateFromResources(resourcePath, createModel(Maps.newHashMap()));
    }

    /**
     * Renders a template from a classpath resource with Jtwig using a model created from a map.
     *
     * @param resourcePath The path of the resource
     * @param values       A map of values to be injected into the model
     * @return The rendered resource
     */
    public static String renderTemplateFromResources(String resourcePath, Map<String, Object> values) {
        return renderTemplateFromResources(resourcePath, createModel(values));
    }

    /**
     * Renders a template from a classpath resource with Jtwig.
     *
     * @param resourcePath The path of the resource
     * @param model        The model which should be used for rendering
     * @return The rendered resource
     */
    public static String renderTemplateFromResources(String resourcePath, JtwigModel model) {
        try {
            final JtwigTemplate template = JtwigTemplate.classpathTemplate(resourcePath);
            return template.render(model);
        } catch (ResourceNotFoundException resourceNotFoundException) {
            // Fallback which might be used when running from an IDE.
            try {
                final JtwigTemplate template = JtwigTemplate.fileTemplate(resourcePath);
                return template.render(model);
            } catch (Exception ex) {
                throw resourceNotFoundException;
            }
        }
    }

    /**
     * Returns a model including the values from the {@link Map}.
     *
     * @param values A map of values to be injected into the model
     * @return The Jtwig model.
     */
    public static JtwigModel createModel(Map<String, Object> values) {
        JtwigModel model = JtwigModel.newModel();

        for (String key : values.keySet()) {
            model = model.with(key, values.get(key));
        }

        return model;
    }

}
