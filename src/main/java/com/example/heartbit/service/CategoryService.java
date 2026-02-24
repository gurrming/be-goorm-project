package com.example.heartbit.service;


import com.example.heartbit.domain.Category;
import com.example.heartbit.dto.CategoryDto;
import com.example.heartbit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 종목 전체 조회 (삭제되지 않은 것만)
     */


}
