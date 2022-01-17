package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = int[].class)
public class StringToPIntArrayConverter implements TypeConverter<String, int[]>{

    @Override
    public int[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, int.class)).mapToInt(v -> (int)v).toArray();
        } return null;
    }
    
}
