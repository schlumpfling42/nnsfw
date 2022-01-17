package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = Integer[].class)
public class StringToIntegerArrayConverter implements TypeConverter<String, Integer[]>{

    @Override
    public Integer[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, Integer.class)).toArray(Integer[]::new);
        } return null;
    }
    
}
