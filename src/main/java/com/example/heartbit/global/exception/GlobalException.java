package com.example.heartbit.global.exception;

import com.example.heartbit.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomerException(CustomerException e){
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Business Exception: {}", errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }
}
