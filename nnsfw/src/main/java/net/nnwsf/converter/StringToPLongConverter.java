package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = long.class)
public class StringToPLongConverter implements TypeConverter<String, Long>{

    @Override
    public Long convert(String source) {
        if(source != null) {
            return Long.parseLong(source);
        } else {
            return 0l;
        }
    }
    
}
