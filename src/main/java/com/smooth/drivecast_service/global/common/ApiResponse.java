package com.smooth.drivecast_service.global.common;

import com.smooth.drivecast_service.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }

    public static <T> ApiResponse<T> success(HttpStatus httpStatus, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(httpStatus.value())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .data(data)
                .build();
    }
}
