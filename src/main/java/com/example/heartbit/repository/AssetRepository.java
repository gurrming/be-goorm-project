package com.example.heartbit.repository;

import com.example.heartbit.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // 멤버 memberId로 자산 조회(npe 방지 위해서 Optional)
    Optional<Asset> findByMemberId(Long memberId);

    // 초기에 자산 생성시 이미 memberId에 해당하는 자산이 존재하는지 확인
    boolean existsByMemberId(Long memberId);

}
