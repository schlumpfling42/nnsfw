package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = long[].class)
public class StringToPLongArrayConverter implements TypeConverter<String, long[]>{

    @Override
    public long[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, long.class)).mapToLong(v -> (long)v).toArray();
        } return null;
    }
    
}
