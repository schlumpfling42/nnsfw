package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = Long[].class)
public class StringToLongArrayConverter implements TypeConverter<String, Long[]>{

    @Override
    public Long[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, Long.class)).toArray(Long[]::new);
        } return null;
    }
    
}
