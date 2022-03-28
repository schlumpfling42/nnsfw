package net.nnwsf.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import net.nnwsf.util.MapUtil;
import net.nnwsf.util.TransformerHelper;

public class ConfigurationManager {
	private static Logger log = Logger.getLogger(ConfigurationManager.class.getName());

    private static ConfigurationManager instance;

    public static void init(ClassLoader applicationClassLoader) {
        if(instance == null) {
            instance = new ConfigurationManager();
            instance.internalInit(applicationClassLoader);
        }
    }

    static void init(ConfigurationManager anInstance, ClassLoader applicationClassLoader) {
            instance = anInstance;
            instance.internalInit(applicationClassLoader);
    }

    public static  <T> T get(String key, Class<T> aClass) {
        return aClass.cast(instance.internalGet(key));
    }

    public static  <T> T get(String[] keys, Class<T> aClass) {
        return TransformerHelper.transform(instance.internalGet(keys), aClass);
    }

	public static boolean exists(String[] keys) {
        return instance.internalGet(keys) != null;
	}

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T apply(T annotation) {
        if(annotation == null) {
            return null;
        }
        try {
            return(T) Proxy.newProxyInstance(annotation.getClass().getClassLoader(), new Class[] { annotation.annotationType() }, new ConfigurationInvocationHandler(annotation));

        } catch(Exception e) {
            log.log(Level.WARNING, "Unable to instantiate Annotation of type " + annotation.annotationType(), e);
        }
        return annotation;

    }

    private Map<String, Object> configuration;
    private final Pattern configurationParameterPattern = Pattern.compile("^\\$\\{([a-zA-Z0-9\\.]*)\\}$");

    protected ConfigurationManager() {
    }

    protected final void internalInit(ClassLoader applicationClassLoader) {
        configuration = MapUtil.deepCopy(loadDefaultConfiguration());
        try {
            Map<String, Object> customConfig = loadApplicationConfiguration(applicationClassLoader);
            MapUtil.mergeInto(configuration, customConfig);
            mergeEnvVariables(null, configuration);
        }catch(Exception e) {
            log.log(Level.INFO, "No custom configuration found, using defaults", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeEnvVariables(String key, Map<String, Object> config) {
        String startKey = key == null ? "" : (key + "_");
        config.entrySet().forEach(anEntry -> {
            String aKey = (startKey + anEntry.getKey()).toUpperCase();
            if(anEntry.getValue() instanceof Map) {
                mergeEnvVariables(aKey, (Map<String, Object>)anEntry.getValue());
            } else {
                String envValue = System.getenv(aKey);
                if(envValue != null && envValue.length() > 0) {
                    anEntry.setValue(envValue);
                }
            }
        });
    }

    Map<String, Object> loadDefaultConfiguration() {
        return new Yaml().load(ConfigurationManager.class.getClassLoader().getResourceAsStream("default.yaml"));
    }

    Map<String, Object> loadApplicationConfiguration(ClassLoader applicationClassLoader) {
        try {
            return new Yaml().load(applicationClassLoader.getResourceAsStream("application.yaml"));
        } catch(Exception e) {
            return Map.of();
        }
    }

    private Object internalGet(String[] keys) {
        return getFrom(keys, 0, configuration);
    }
    
    private Object internalGet(String key) {
        Matcher keyMatcher = configurationParameterPattern.matcher(key);
        if(key != null && keyMatcher.matches()) {
            String[] keyElements = keyMatcher.group(1).split("\\.");
            return getFrom(keyElements, 0, configuration);
        }
        return key;
    }

    private Object getFrom(String[] keys, int index, Object element) {
        if(keys != null) {
            if(index == keys.length) {
                return element;
            }
            if(Map.class.isInstance(element)) {
                return getFrom(keys, index + 1, ((Map<?,?>)element).get(keys[index]));
            }
        }
        return null;
    }
}