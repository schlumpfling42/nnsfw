package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = Integer.class)
public class StringToIntegerConverter implements TypeConverter<String, Integer>{

    @Override
    public Integer convert(String source) {
        if(source != null) {
            return Integer.parseInt(source);
        } return null;
    }
    
}
