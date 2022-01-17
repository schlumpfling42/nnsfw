package net.nnwsf.converter;

import net.nnwsf.converter.annotation.Converter;

@Converter(sourceType = String.class, targetType = int.class)
public class StringToPIntConverter implements TypeConverter<String, Integer>{

    @Override
    public Integer convert(String source) {
        if(source != null) {
            return Integer.parseInt(source);
        } return 0;
    }
    
}
