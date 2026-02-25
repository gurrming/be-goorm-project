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
        Analysis analysis = analysisRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과 없음"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리 없음"));

        AnalysisDTO dto = AnalysisDTO.builder()
                .symbol(category.getSymbol())
                .totalResult(analysis.getTotalScore())
                .totalLabel(analysis.getTotalLabel())
                .newsResult(analysis.getNewsResult())       // 수정된 Getter
                .communityResult(analysis.getCommunityResult()) // 수정된 Getter
                .fullReport(analysis.getFullReport())   // ★ 상세 리포트 넣기
                .summary(analysis.getSummary())             // [추가]
                .rsi(analysis.getRsi())                     // [추가]
                .build();

        return List.of(dto);
    }
}