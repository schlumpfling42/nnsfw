package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = Double.class)
public class StringToDoubleConverter implements TypeConverter<String, Double>{

    @Override
    public Double convert(String source) {
        if(source != null) {
            return Double.parseDouble(source);
        } return null;
    }
    
}
