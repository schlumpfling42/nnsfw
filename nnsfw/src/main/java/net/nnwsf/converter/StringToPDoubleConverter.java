package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = double.class)
public class StringToPDoubleConverter implements TypeConverter<String, Double>{

    @Override
    public Double convert(String source) {
        if(source != null) {
            return Double.parseDouble(source);
        } return 0d;
    }
    
}
