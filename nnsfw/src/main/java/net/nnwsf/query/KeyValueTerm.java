package net.nnwsf.query;

public class KeyValueTerm extends SearchTerm {

    private final String key;
    private final String value;

    public KeyValueTerm(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + ":" + value;
    }
}