package com.smooth.alert_service.global.exception;

import com.smooth.alert_service.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("BusinessException occurred: {} at {}", e.getMessage(), request.getRequestURI());

        String message = e.getCustomMessage() != null ? e.getCustomMessage() : e.getErrorCode().getMessage();
        ApiResponse<Object> response = ApiResponse.error(e.getErrorCode(), message);

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(Exception e) {
        log.warn("Validation exception occurred: {}", e.getMessage());

        List<String> errors;
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            errors = ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
        } else {
            BindException ex = (BindException) e;
            errors = ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
        }

        ApiResponse<List<String>> response = ApiResponse.error(
                CommonErrorCode.UNPROCESSABLE_ENTITY,
                "입력 데이터 검증에 실패했습니다.",
                errors
        );

        return ResponseEntity
                .status(CommonErrorCode.UNPROCESSABLE_ENTITY.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch exception occurred: {}", e.getMessage());

        String message = String.format("'%s' 파라미터의 값이 올바르지 않습니다.", e.getName());
        ApiResponse<Object> response = ApiResponse.error(CommonErrorCode.BAD_REQUEST, message);

        return ResponseEntity
                .status(CommonErrorCode.BAD_REQUEST.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception occurred: {} at {}", e.getMessage(), request.getRequestURI(), e);

        ApiResponse<Object> response = ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(response);
    }
}
