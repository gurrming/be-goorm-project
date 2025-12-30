package com.example.heartbit.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
public class InterestDto {

    private Long interestId;
    private Long memberId;
    private Long categoryId;

    @Builder
    public InterestDto(Long interestId, Long memberId, Long categoryId) {
        this.interestId = interestId;
        this.memberId = memberId;
        this.categoryId = categoryId;
    }

}
