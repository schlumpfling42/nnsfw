package net.nnwsf.resource;

public class PageRequest {

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
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
