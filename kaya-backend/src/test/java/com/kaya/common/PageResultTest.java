package com.kaya.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResultTest {

    @Test
    void calculatesTotalPages() {
        PageResult<String> result = PageResult.of(List.of("a"), 1, 10, 12);

        assertEquals(2, result.getTotalPages());
        assertEquals(12, result.getTotal());
    }

    @Test
    void handlesZeroPageSizeWithoutDividingByZero() {
        PageResult<String> result = PageResult.of(List.of(), 1, 0, 0);

        assertEquals(0, result.getTotalPages());
    }
}
