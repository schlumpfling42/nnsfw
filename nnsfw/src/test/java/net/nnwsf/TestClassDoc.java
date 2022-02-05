package net.nnwsf;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;

import net.nnwsf.controller.documentation.model.BeanClassDescription;
import net.nnwsf.controller.documentation.model.ClassDescription;
import net.nnwsf.controller.documentation.model.CollectionClassDescription;
import net.nnwsf.controller.documentation.model.MapClassDescription;
import net.nnwsf.controller.documentation.model.SimpleClassDescription;
import net.nnwsf.util.ReflectionHelper;
import net.nnwsf.util.TemplateUtil;

public class TestClassDoc {

    static final Collection<Class<?>> simpleTypes = Set.of(String.class, Integer.class, Long.class, Double.class, Float.class, BigDecimal.class, BigInteger.class, int.class, long.class, float.class, double.class, Character.class, char.class, CharSequence.class);

    public static class TestBean {
        String name;
        Collection<TestElement> elements;
        Collection<String> strings;
        TestElement element;
        Map<TestElement, TestElement> elementMap;
        Map<String, Integer> stringIntegerMap;
    }

    public static class TestElement {
        String name;
    }

    @Test
    void testClass() throws JsonProcessingException {
        ClassDescription classRepresentation = getClassDescription(TestBean.class);

        System.out.println(classRepresentation.asString());
    }

    private ClassDescription getClassDescription(Class<?> aClass) {
        if(simpleTypes.contains(aClass)) {
            return SimpleClassDescription.of(aClass);
        }
        return BeanClassDescription.of(Arrays.stream(aClass.getDeclaredFields())
            .map(this::getClassDescription)
            .collect(Collectors.toMap(
                        Entry::getKey,
                        Entry::getValue)));
    }

    private Map.Entry<String, ClassDescription> getClassDescription(Field aField) {
        if(aField.getType().isAssignableFrom(Collection.class)) {
            Class<?> genericClass = ReflectionHelper.getGenericType(aField);
            return Map.entry(aField.getName(), CollectionClassDescription.of(getClassDescription(genericClass)));
        } else if(aField.getType().isAssignableFrom(Map.class)) {
            Class<?>[] genericClasses = ReflectionHelper.getGenericTypes(aField);
            return Map.entry(aField.getName(), MapClassDescription.of(getClassDescription(genericClasses[0]), getClassDescription(genericClasses[1])));
        } else if(simpleTypes.contains(aField.getType())) {
            return Map.entry(aField.getName(), SimpleClassDescription.of(aField.getType()));
        } else {
            return Map.entry(aField.getName(), getClassDescription(aField.getType()));
        }
    }

    @Test
    public void testTemplate() {
        String result = TemplateUtil.render("api_parameter.jte", getClassDescription(TestBean.class));
        System.out.println(result);
    }
}
