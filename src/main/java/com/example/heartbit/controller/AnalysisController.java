package com.example.heartbit.controller;

//AI 서비스

import com.example.heartbit.dto.AnalysisDTO;
import com.example.heartbit.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis") //프론트가 호출하는 url
@RequiredArgsConstructor
@Tag(name = "AI 기능 API")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "감성 분석(긍/부정) 조회", description = "각 symbol마다 감성 분석에 대한 결과를 나타냅니다.")
    @GetMapping("/{categoryID}")
    public ResponseEntity<List<AnalysisDTO>> getSentiment(@PathVariable Long categoryID) {
        return ResponseEntity.ok(analysisService.getSentimentData(categoryID));
    }

}