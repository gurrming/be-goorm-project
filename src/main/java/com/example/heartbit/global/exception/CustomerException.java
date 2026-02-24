package com.example.heartbit.global.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomerException extends RuntimeException {
    private final ErrorCode errorCode;
}
