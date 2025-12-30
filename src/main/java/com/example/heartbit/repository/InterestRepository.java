package com.example.heartbit.repository;


import com.example.heartbit.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    List<Interest> findByMembertId(Long membertId);
    boolean existsByMembertIdAndCategoryId(Long membertId, Long categoryId);
    void deleteByMembertIdAndCategoryId(Long membertId, Long categoryId);
}
