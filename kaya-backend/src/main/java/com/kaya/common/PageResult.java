package com.kaya.common;

import java.util.List;

/**
 * Pagination shape consumed by the frontend:
 * {@code { items, page, pageSize, total, totalPages }}.
 */
public class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
    private final int totalPages;

    public PageResult(List<T> items, int page, int pageSize, long total, int totalPages) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = totalPages;
    }

    public static <T> PageResult<T> of(List<T> items, int page, int pageSize, long total) {
        int totalPages = pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        return new PageResult<>(items, page, pageSize, total, totalPages);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
