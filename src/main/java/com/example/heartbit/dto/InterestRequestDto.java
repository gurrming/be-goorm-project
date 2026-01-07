package com.example.heartbit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class InterestRequestDto {

    public record Create(
            @Schema(description = "멤버 ID", example = "3")
            Long memberId,

            @Schema(description = "종목 ID", example = "10")
            Long categoryId
    ) {}
}