package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = double[].class)
public class StringToPDoubleArrayConverter implements TypeConverter<String, double[]>{

    @Override
    public double[] convert(String source) {
        if(source != null) {
            return Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, double.class)).mapToDouble(v -> (double)v).toArray();
        } return null;
    }
    
}
