package com.smooth.drivecast_service.driving.feign;

import com.smooth.drivecast_service.driving.dto.TraitResponseDto;
import com.smooth.drivecast_service.driving.dto.TraitBulkResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 유저 서비스 성향 조회 Feign 클라이언트
 */
@FeignClient(
        name = "driving-analysis-service",
        url = "${app.client.driving-analysis-service.url}",
        path = "/internal/v1"
)
public interface UserTraitClient {

    /**
     * 단건 성향 조회
     */
    @GetMapping("/traits/{userId}") // 원래는 /characters/{userId}
    TraitResponseDto getTrait(@PathVariable String userId);

    /**
     * 벌크 성향 조회
     */
    @GetMapping("/traits/bulk") // 원래는 /characters
    TraitBulkResponseDto getTraitsBulk(@RequestParam(defaultValue = "true") boolean hasCharacter);
}