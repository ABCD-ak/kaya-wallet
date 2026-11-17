package com.kaya.mapper;

import com.kaya.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    /**
     * @param type {@code income}, {@code expense}, or {@code null} for all active categories
     */
    List<Category> findAll(@Param("type") String type);

    Category findById(@Param("id") Long id);
}
