package com.example.heartbit.global.exception;

import com.example.heartbit.global.response.CustomResponse;
import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getStatus();
    String getCode();
    String getMessage();

    default CustomResponse<Void> getErrorResponse() {
        return CustomResponse.failure(getCode(), getMessage());
    }
}