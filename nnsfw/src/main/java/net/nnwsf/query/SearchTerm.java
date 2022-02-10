package net.nnwsf.query;

public abstract class SearchTerm {

    static OperatorTerm operator(SearchTerm right, String value) {
        return new OperatorTerm(right, value);
    }

    static KeyValueTerm keyValue(String key, String value) {
        return new KeyValueTerm(key, value);
    }
}