package net.nnwsf.resource;

import java.util.Collection;

public class Page<T> {

    private final long totalNumber;

    private final int size;
    private final int pageNumber;
    private final Collection<T> elements;

    public Page(long totalNumber, PageRequest request, Collection<T> elements) {
        this.totalNumber = totalNumber;
        this.size = request.getSize();
        this.pageNumber = request.getPage();
        this.elements = elements;
    }

    public Collection<T> getElements() {
        return elements;
    }
    public int getSize() {
        return size;
    }
    public int getPageNumber() {
        return pageNumber;
    }
    public long getTotalNumber() {
        return totalNumber;
    }
}
