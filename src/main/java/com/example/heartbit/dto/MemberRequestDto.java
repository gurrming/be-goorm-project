package com.example.heartbit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class MemberRequestDto {
    public record Signup(
            @NotBlank String memberEmail,
            @NotBlank String memberPassword,
            @NotBlank String memberNickname
    ){
        public String getEmail() {
            return memberEmail;
        }
        public String getPassword(){
            return memberPassword;
        }
        public String getNickname(){
            return memberNickname;
        }
    }

    public record Login(
            @NotBlank String memberEmail,
            @NotBlank String memberPassword
    ){
        public String getPassword(){
            return memberPassword;
        }
    }

}
