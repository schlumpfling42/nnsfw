package net.nnwsf.util;

import java.util.HashMap;
import java.util.Map;

public class MapUtil {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        source.keySet().forEach(key -> {
            if(source.get(key) != null && source.get(key) instanceof Map) {
                result.put(key, deepCopy((Map<String, Object>)source.get(key)));
            } else {
                result.put(key, source.get(key));
            }
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void mergeInto(Map<String, Object> destination, Map<String, Object> source) {
        if(source != null) {
            source.keySet().forEach(key -> {
                if(destination.get(key) != null && destination.get(key) instanceof Map) {
                    mergeInto((Map<String, Object>)destination.get(key), (Map<String, Object>)source.get(key));
                } else {
                    destination.put(key, source.get(key));
                }
            });
        }
    }
}
