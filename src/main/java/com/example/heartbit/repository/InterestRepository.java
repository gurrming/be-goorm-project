package com.example.heartbit.repository;


import com.example.heartbit.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    List<Interest> findByMemberId(Long memberId);
    boolean existsByMemberIdAndCategoryId(Long memberId, Long categoryId);
    void deleteByMemberIdAndCategoryId(Long memberId, Long categoryId);
}
