package com.example.heartbit.repository;

import com.example.heartbit.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 삭제되지 않은 종목만 조회
    List<Category> findByCategoryDeleteFalse();
}
