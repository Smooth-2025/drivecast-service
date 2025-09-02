package com.smooth.drivecast_service.global.util;

import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.global.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class AuthenticationUtils {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String AUTHENTICATED_HEADER = "X-Authenticated";

    public static Long getCurrentUserId() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            String userIdHeader = request.getHeader(USER_ID_HEADER);
            String authenticatedHeader = request.getHeader(AUTHENTICATED_HEADER);

            if (StringUtils.hasText(userIdHeader) && "true".equals(authenticatedHeader)) {
                return Long.valueOf(userIdHeader);
            }
        } catch (Exception e) {
            log.debug("HTTP 헤더에서 사용자 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    public static Long getCurrentUserIdOrThrow() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}