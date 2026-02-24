package com.example.heartbit.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor

public enum OrderStatus {
    OPEN("미체결"),
    PARTIAL("부분체결"),
    FILLED("체결"),
    CANCELLED("취소");

    private final String text;
    }
