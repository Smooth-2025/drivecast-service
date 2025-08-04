package com.smooth.alert_service.controller;

import com.smooth.alert_service.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile("dev") // 개발 환경에서만 활성화
public class TestTokenController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/test-token")
    public ResponseEntity<?> generateTestToken(@RequestParam String userId) {

        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.badRequest().body("userId는 필수입니다.");
        }
        
        if (userId.length() > 50) {
            return ResponseEntity.badRequest().body("userId는 50자를 초과할 수 없습니다.");
        }

        try {
            String token = jwtTokenProvider.createTestToken(userId);
            return ResponseEntity.ok(new TokenResponse("Bearer " + token, userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("토큰 생성 실패: " + e.getMessage());
        }
    }
    
    // 응답 DTO
    public record TokenResponse(String token, String userId) {}
}
