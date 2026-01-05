package com.example.heartbit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class MemberRequestDto {
    public record Signup(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String nickname
    ){ }

    public record Login(
            @NotBlank String email,
            @NotBlank String password
    ){ }

}
