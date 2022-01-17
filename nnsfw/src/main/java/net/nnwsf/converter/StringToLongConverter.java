package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = Long.class)
public class StringToLongConverter implements TypeConverter<String, Long>{

    @Override
    public Long convert(String source) {
        return Long.parseLong(source);
    }
    
}
