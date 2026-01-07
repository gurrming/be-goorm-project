package com.example.heartbit.repository;

import com.example.heartbit.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // symbol 기준 대소문자 무시 조회
    Optional<Category> findBySymbolIgnoreCase(String symbol);

    // 삭제되지 않은 종목만 조회
    List<Category> findByCategoryDeleteFalse();
}
