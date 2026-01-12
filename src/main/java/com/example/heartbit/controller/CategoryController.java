package com.example.heartbit.controller;

import com.example.heartbit.dto.CategoryDto;
import com.example.heartbit.service.CategoryService;
import com.example.heartbit.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 종목 조회 API 컨트롤러
 * - 전체 종목 리스트 조회
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "종목", description = "투자 가능한 종목(카테고리) 조회 API")
public class CategoryController {

    private final CategoryService categoryService;
    private final TradeService tradeService;

    @GetMapping("/categories")
    @Operation(
            summary = "전체 종목 조회",
            description = "모든 투자 가능 종목 목록을 조회합니다."
    )
    public List<CategoryDto> getCategories() {
        return tradeService.getCategories();
    }

    @GetMapping("/category")
    @Operation(summary = "종목 단건 조회", description = "종목을 단건으로 조회합니다.")
    public ResponseEntity<CategoryDto> getCategory(@RequestParam Long categoryId) {
        CategoryDto response = tradeService.getCategory(categoryId);
        return ResponseEntity.ok(response);
    }
}
