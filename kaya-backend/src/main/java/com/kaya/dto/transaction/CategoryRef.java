package com.kaya.dto.transaction;

import com.kaya.entity.Category;

public record CategoryRef(
        String id,
        String code,
        String type,
        String name,
        String icon,
        String color,
        Integer sortOrder
) {

    public static CategoryRef from(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryRef(
                String.valueOf(category.getId()),
                category.getCode(),
                category.getType(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getSortOrder());
    }
}
