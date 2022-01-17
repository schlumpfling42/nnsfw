package net.nnwsf.converter;

import java.util.Arrays;

import net.nnwsf.converter.annotation.Converter;
import net.nnwsf.util.TypeUtil;

@Converter(sourceType = String.class, targetType = float[].class)
public class StringToPFloatArrayConverter implements TypeConverter<String, float[]>{

    @Override
    public float[] convert(String source) {
        if(source != null) {
            double[] result = Arrays.stream(source.split(",")).map(aString -> TypeUtil.toType(aString, float.class)).mapToDouble(v -> (double)v).toArray();
            float[] floatResult = new float[result.length];
            for(int i=0; i<result.length; i++) {
                floatResult[i] = (float)result[i];
            }
            return floatResult;
        } return null;
    }
    
}
