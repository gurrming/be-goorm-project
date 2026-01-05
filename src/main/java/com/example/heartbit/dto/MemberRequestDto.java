package com.example.heartbit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class MemberRequestDto {
    public record Signup(

            @Email @NotBlank
            @Schema(description = "이메일", example = "test@example.com")
            String email,

            @NotBlank
            @Schema(description = "비밀번호", example = "test1234")
            String password,
            
            @Schema(description = "닉네임", example = "윤모씨")
            @NotBlank String nickname
    ){ }

    public record Login(
            @NotBlank
            @Schema(description = "이메일", example = "test@example.com")
            String email,

            @NotBlank
            @Schema(description = "비밀번호", example = "test1234")
            String password
    ){ }

}
