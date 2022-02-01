package net.nnwsf.util;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;

public class TemplateUtil {
    private static TemplateUtil instance = new TemplateUtil();
    public static String render(String template, Object model) {
        return instance.internalRender(template, model);
    }

    private final TemplateEngine templateEngine = TemplateEngine.createPrecompiled(gg.jte.ContentType.Html);

    private String internalRender(String template, Object model) {
        TemplateOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        return output.toString();
    }
}
