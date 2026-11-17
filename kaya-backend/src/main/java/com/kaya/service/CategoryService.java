package com.kaya.service;

import com.kaya.common.PageResult;
import com.kaya.entity.Category;

public interface CategoryService {

    PageResult<Category> page(String type, int page, int pageSize);

    Category getById(Long id);
}
