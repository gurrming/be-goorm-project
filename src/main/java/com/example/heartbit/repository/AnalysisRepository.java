package com.example.heartbit.repository;

import com.example.heartbit.domain.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByCategoryId(Long categoryId);
}
