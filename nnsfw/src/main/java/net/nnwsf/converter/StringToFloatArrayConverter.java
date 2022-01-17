package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = Float[].class)
public class StringToFloatArrayConverter implements TypeConverter<String, Float[]>{

    @Override
    public Float[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, Float.class)).toArray(Float[]::new);
        } return null;
    }
    
}
