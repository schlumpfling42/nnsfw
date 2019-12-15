package net.nnwsf.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nnwsf.util.ReflectionHelper;
import net.sf.cglib.proxy.Proxy;

public class ConfigurationInvocationHandler implements InvocationHandler {
    private static Logger log = Logger.getLogger(ConfigurationInvocationHandler.class.getName());
    
    private final static Pattern configurationContainerParameterPattern = Pattern.compile("^\\$\\{([a-zA-Z0-9\\.]*)\\}.*$");
    
    private final Object object;
    private final String configurationName;
    private final Map<String, String> containerElements = new HashMap<>();

    ConfigurationInvocationHandler(Object object) {
        this.object = object;
        this.configurationName = ReflectionHelper.findAnnotation(object.getClass(), ConfigurationKey.class).value();
        Map<Annotation, Method> configurationKeyMethods = ReflectionHelper.findAnnotationMethods(object.getClass(), ConfigurationKey.class);
        for(Entry<Annotation, Method> entry : configurationKeyMethods.entrySet()) {
            if(ConfigurationKey.class.isAssignableFrom(entry.getKey().getClass()) && ConfigurationKey.class.cast(entry.getKey()).containsKeys()) {
                try {
                    String value = entry.getValue().invoke(object, new Object[0]).toString();
                    boolean configurationExists = ConfigurationManager.exists(new String[] {configurationName, value});
                    if(!Objects.equals(value, entry.getValue().getDefaultValue()) || configurationExists) {
                        containerElements.put("${" + ConfigurationKey.class.cast(entry.getKey()).value() + "}", value);
                    }
                } catch(Exception e) {
                    log.log(Level.SEVERE, "unable to get value for containter key {}", ConfigurationKey.class.cast(entry.getKey()).value());
                }
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ConfigurationKey key = ReflectionHelper.findAnnotation(method, ConfigurationKey.class);

        String keyValue = key.value();
        Matcher keyMatcher = configurationContainerParameterPattern.matcher(keyValue);
        if(keyMatcher.matches()) {
            String containerKey = "${" + keyMatcher.group(1) + "}";
            String containerKeyValue = containerElements.get(containerKey);
            if(containerKeyValue == null) {
                keyValue = keyValue.replace(containerKey + ".", "");
            } else {
                keyValue = keyValue.replace(containerKey, containerKeyValue);
            }
        }

        Object codeValue = method.invoke(object, args);
        if(codeValue == null || codeValue.equals(method.getDefaultValue())) {
            if(key != null) {
                List<String> elements = new ArrayList<>();
                elements.add(configurationName);
                elements.addAll(Arrays.asList(keyValue.split("\\.")));
                Object configurationValue = ConfigurationManager.get(elements.toArray(new String[elements.size()]), method.getReturnType());
                if(configurationValue != null) {
                    return configurationValue;
                }
            }
        }
        return codeValue;
    }
}