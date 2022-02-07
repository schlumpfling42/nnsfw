package net.nnwsf.util;

public class Pair<F, S> {
    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<F, S>(first, second);
    }

    private final F first;
    private final S second; 

    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public S getSecond() {
        return second;
    }

    public F getFirst() {
        return first;
    }
}
