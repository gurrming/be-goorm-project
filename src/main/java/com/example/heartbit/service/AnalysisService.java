package com.example.heartbit.service;

import com.example.heartbit.domain.Analysis;
import com.example.heartbit.domain.Category;
import com.example.heartbit.dto.AnalysisDTO;
import com.example.heartbit.repository.AnalysisRepository;
import com.example.heartbit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // 추가됨

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public List<AnalysisDTO> getSentimentData(Long categoryId) {
        // 1. 분석 결과 조회
        Analysis analysis = analysisRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 종목의 분석 결과가 없습니다."));

        // 2. 카테고리 정보 조회
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 없습니다."));

        // 3. DTO 생성 후 리스트로 감싸서 반환
        AnalysisDTO dto = AnalysisDTO.builder()
                .symbol(category.getSymbol())
                .totalResult(analysis.getTotalScore())
                .totalLabel(analysis.getTotalLabel())
                .newsResult(analysis.getNewsScore())
                .communityResult(analysis.getCommunityScore())
                .build();

        return List.of(dto);
    }
}