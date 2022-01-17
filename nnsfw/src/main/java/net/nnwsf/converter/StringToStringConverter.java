package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = String.class)
public class StringToStringConverter implements TypeConverter<String, String>{

    @Override
    public String convert(String source) {
        return source;
    }
    
}
