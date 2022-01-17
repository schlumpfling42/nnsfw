package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = Float[].class)
public class StringToDoubleArrayConverter implements TypeConverter<String, Double[]>{

    @Override
    public Double[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, Double.class)).toArray(Double[]::new);
        } return null;
    }
    
}
