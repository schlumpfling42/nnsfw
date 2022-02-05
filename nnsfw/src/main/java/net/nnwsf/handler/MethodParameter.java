package net.nnwsf.handler;

public class MethodParameter {

    private final int index;
    private final String name;
    private final Class<?> type;

    public MethodParameter(String name, Class<?> type, int index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MethodParameter [name=" + name + ", type=" + type + "]";
    }

}
