package com.example.heartbit.repository;


import com.example.heartbit.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    List<Interest> findByMember_MemberId(Long memberId);
    boolean existsByMember_MemberIdAndCategory_CategoryId(Long memberId, Long categoryId);
    void deleteByMember_MemberIdAndCategory_CategoryId(Long memberId, Long categoryId);
}
