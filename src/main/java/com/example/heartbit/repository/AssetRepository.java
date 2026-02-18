package com.example.heartbit.repository;

import com.example.heartbit.domain.Asset;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // 멤버 memberId로 자산 조회(npe 방지 위해서 Optional)
    Optional<Asset> findByMember_MemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.member.memberId = :memberId")
    Optional<Asset> findByMemberIdWithLock(Long memberId);


}
