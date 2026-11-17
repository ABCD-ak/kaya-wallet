package com.kaya.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.kaya.common.BusinessException;
import com.kaya.common.PageResult;
import com.kaya.entity.Category;
import com.kaya.mapper.CategoryMapper;
import com.kaya.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_TYPES = Set.of("income", "expense");

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Category> page(String type, int page, int pageSize) {
        String normalizedType = normalizeType(type);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        PageHelper.startPage(safePage, safePageSize);
        List<Category> categories = categoryMapper.findAll(normalizedType);
        PageInfo<Category> pageInfo = new PageInfo<>(categories);

        return new PageResult<>(
                pageInfo.getList(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getPages());
    }

    @Override
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException(40401, "分类不存在");
        }
        return category;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalized = type.trim().toLowerCase();
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new BusinessException(40001, "type 只能是 income 或 expense");
        }
        return normalized;
    }
}
