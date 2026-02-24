package com.example.heartbit.controller;

import com.example.heartbit.dto.InterestRequestDto;
import com.example.heartbit.dto.InterestResponseDto;
import com.example.heartbit.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
@Tag(name = "관심종목 관리 API")
public class InterestController {

    private final InterestService interestService;

    // 1) 멤버별 관심 추가
    @Operation(summary = "멤버별 관심 생성", description = "멤버 ID를 통해 관심 종목을 추가합니다.")
    @PostMapping
    public ResponseEntity<InterestResponseDto> interestAdd(
            @RequestBody InterestRequestDto.Create request
    ) {
        InterestResponseDto savedInterest =
                interestService.interestAdd(request.memberId(), request.categoryId());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedInterest);
    }


    // 2) 멤버별 관심 목록 조회
    @Operation(summary = "멤버별 관심 목록 조회", description = "멤버 ID를 통해 관심 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<InterestResponseDto>> interestList(@RequestParam Long memberId) {
        List<InterestResponseDto> interests = interestService.getInterest(memberId);
        return ResponseEntity.ok(interests);
    }


    // 3) 멤버별 관심 삭제
    @Operation(summary = "멤버별 관심 목록 삭제", description = "관심 목록을 삭제합니다.")
    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> interestDelete(@PathVariable Long interestId) {
        interestService.delete(interestId);
        // 204 No Content
        return ResponseEntity.noContent().build();
    }
}
