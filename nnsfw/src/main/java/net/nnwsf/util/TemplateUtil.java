package net.nnwsf.util;

import java.util.Map;

public class TemplateUtil {
    public static String fill(String template, Map<String, String> replacements) {
        String result = template;
        for(String key : replacements.keySet()) {
            result = result.replace("{" + key + "}", replacements.get(key));
        }
        return result;
    }
}
