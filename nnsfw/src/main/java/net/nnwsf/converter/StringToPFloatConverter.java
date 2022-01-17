package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = float.class)
public class StringToPFloatConverter implements TypeConverter<String, Float>{

    @Override
    public Float convert(String source) {
        if(source != null) {
            return Float.parseFloat(source);
        } return 0f;
    }
    
}
