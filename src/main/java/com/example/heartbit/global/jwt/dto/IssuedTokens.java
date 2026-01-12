package com.example.heartbit.global.jwt.dto;

public record IssuedTokens (
    String accessToken,
    String refreshToken,
    long accessExpiresInSec,
    long refreshExpiresInSec
){}


