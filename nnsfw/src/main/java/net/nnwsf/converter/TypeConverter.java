package net.nnwsf.converter;

public interface TypeConverter<S,T> {
    public T convert(S source);
}
