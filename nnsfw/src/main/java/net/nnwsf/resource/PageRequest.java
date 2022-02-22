package net.nnwsf.resource;

public class PageRequest {

    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(page == null ? 0 : page, size == null ? 100 : size);
    }
    private final int page;
    private final int size;
    
    private PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public int getPage() {
        return page;
    }

    
}
