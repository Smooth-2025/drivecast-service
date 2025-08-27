package com.smooth.drivecast_service.emergency.service;

import com.smooth.drivecast_service.emergency.feign.UserServiceClient;
import com.smooth.drivecast_service.emergency.feign.dto.EmergencyInfoResponse;
import com.smooth.drivecast_service.emergency.service.mapper.EmergencyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smooth.drivecast_service.emergency.dto.AccidentInfoDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyReportResult;
import com.smooth.drivecast_service.emergency.dto.EmergencyRequestDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyUserInfoDto;
import com.smooth.drivecast_service.emergency.entity.EmergencyReport;
import com.smooth.drivecast_service.emergency.exception.EmergencyErrorCode;
import com.smooth.drivecast_service.emergency.feign.dto.EmergencyReportDto;
import com.smooth.drivecast_service.emergency.repository.EmergencyReportRepository;
import com.smooth.drivecast_service.global.common.cache.DedupService;
import com.smooth.drivecast_service.global.exception.BusinessException;

import com.smooth.drivecast_service.incident.dto.IncidentEvent;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyReportService {

    private final EmergencyReportRepository emergencyReportRepository;
    private final SmsService smsService;
    private final UserServiceClient userServiceClient;
    private final DedupService dedupService;
    private final ObjectMapper objectMapper;
    private final EmergencyMapper emergencyMapper;
    
    private static final String SMS_119_FORMAT = "[응급상황 119 신고]\n환자정보: %s/%s형\n사고시간: %s\n위치: 위도 %.6f, 경도 %.6f\n연락처: 자동신고시스템";
    private static final String SMS_FAMILY_FORMAT = "[응급상황 알림]\n가족에게 알림: 교통사고 발생\n사고시간: %s\n위치: 위도 %.6f, 경도 %.6f\n상황: 자동 119 신고 완료";

    @Transactional
    public EmergencyReportResult processEmergencyDecision(EmergencyRequestDto req, String jwtUserId) {
        final String accidentId = req.getAccidentId();
        final Long userId = jwtUserId != null ? Long.parseLong(jwtUserId) : req.getUserId();

        log.info("응급 신고 여부 판단 : accidentId = {}, userId = {}, JWT userId = {}, 신고 여부 = {}, 타임 아웃 = {} ", accidentId, userId, jwtUserId, req.getChooseReport(), req.isTimeout());

        try {
            if (isAlreadyReported(accidentId, userId)) {
                throw new BusinessException(EmergencyErrorCode.EMERGENCY_REPORT_ALREADY_EXISTS);
            }

            EmergencyReport report = createEmergencyReport(req, userId);

            if (!report.getEmergencyNotified()) {
                return EmergencyReportResult.cancelled(accidentId, "신고가 취소되었습니다.");
            }

            return processEmergencyReport(report, accidentId);

        } catch (BusinessException businessException) {
            throw businessException;
        } catch (Exception e) {
            log.error("응급 신고 처리 중 에러 발생", e);
            throw new BusinessException(EmergencyErrorCode.EMERGENCY_REPORT_PROCESSING_FAILED, e.getMessage(), e);
        }
    }

    private EmergencyReport createEmergencyReport(EmergencyRequestDto req, Long userId) {
        EmergencyReport report = new EmergencyReport();
        report.setAccidentId(req.getAccidentId());
        report.setUserId(userId);
        
        boolean shouldNotify = isEmergencyReportRequested(req);
        report.setEmergencyNotified(shouldNotify);
        return emergencyReportRepository.save(report);
    }

    private EmergencyReportResult processEmergencyReport(EmergencyReport report, String accidentId) {
        log.info("실제 응급신고 처리 시작");

        AccidentInfoDto accidentInfo = getAccidentInfo(accidentId);
        if (accidentInfo == null) {
            throw new BusinessException(EmergencyErrorCode.EMERGENCY_ACCIDENT_INFO_NOT_FOUND);
        }

        EmergencyUserInfoDto userInfo = getUserInfo(report.getUserId());

        sendEmergencySms(accidentInfo, userInfo);
        boolean familyNotified = sendFamilyNotification(userInfo, accidentInfo);

        report.setFamilyNotified(familyNotified);
        emergencyReportRepository.save(report);

        return EmergencyReportResult.reported(accidentId, "119 신고가 접수되었습니다.");
    }

    private AccidentInfoDto getAccidentInfo(String accidentId) {
        try {
            String accidentJson = dedupService.getAccidentInfo(accidentId);
            if (accidentJson == null) {
                log.warn("캐시에서 사고 정보를 찾을 수 없음: accidentId={}", accidentId);
                return null;
            }
            
            IncidentEvent incidentEvent = objectMapper.readValue(accidentJson, IncidentEvent.class);
            return AccidentInfoDto.fromAlertEvent(incidentEvent);
        } catch (Exception e) {
            log.error("사고 정보 조회 실패: accidentId={}", accidentId, e);
            throw new BusinessException(EmergencyErrorCode.EMERGENCY_ACCIDENT_INFO_NOT_FOUND, "사고 정보 조회 중 에러가 발생했습니다.");
        }
    }

    @Transactional
    public boolean isAlreadyReported(String accidentId, Long userId) {
        boolean exists = emergencyReportRepository
                .existsByAccidentIdAndUserIdAndEmergencyNotifiedTrue(accidentId, userId);
        log.debug("중복 신고 체크: accidentId={}, userId={}, exists={}", accidentId, userId, exists);
        return exists;
    }

    private boolean isEmergencyReportRequested(EmergencyRequestDto req) {
        return req.getChooseReport() || req.isTimeout();
    }

    
    private EmergencyUserInfoDto getUserInfo(Long userId) {
        try {
            EmergencyInfoResponse response = userServiceClient.getUserInfo(String.valueOf(userId));
            return emergencyMapper.toUserInfoDto(response.getData());
        } catch (Exception e) {
            log.error("유저 정보 조회 실패: userId={}", userId, e);
            throw new BusinessException(EmergencyErrorCode.EMERGENCY_USER_SERVICE_ERROR, "사용자 정보 조회 중 외부 서비스 에러가 발생했습니다.");
        }
    }
    
    private void sendEmergencySms(AccidentInfoDto accidentInfo, EmergencyUserInfoDto userInfo) {
        String gender = formatGender(userInfo.getGender());
        String bloodType = formatBloodType(userInfo.getBloodType());
        
        String message = String.format(
            SMS_119_FORMAT,
            gender,
            bloodType,
            accidentInfo.getAccidentTime(),
            accidentInfo.getLatitude(),
            accidentInfo.getLongitude()
        );
        
        boolean sent = smsService.send119(message);
        if (!sent) {
            throw new BusinessException(EmergencyErrorCode.EMERGENCY_SMS_119_FAILED);
        }
        log.info("119 신고 SMS 발송 완료");
    }
    
    private boolean sendFamilyNotification(EmergencyUserInfoDto userInfo, AccidentInfoDto accidentInfo) {
        if (!userInfo.canNotifyFamily()) {
            log.info("가족 연락처가 없어서 가족 알림 생략");
            return false;
        }
        
        String message = String.format(
            SMS_FAMILY_FORMAT,
            accidentInfo.getAccidentTime(),
            accidentInfo.getLatitude(),
            accidentInfo.getLongitude()
        );
        
        boolean familySent = smsService.sendFamily(userInfo, message);
        if (!familySent) {
            log.warn("가족 SMS 발송에 실패했지만 처리를 계속합니다.");
        } else {
            log.info("가족 알림 SMS 발송 완료");
        }
        return familySent;
    }

    public List<EmergencyReportDto> getUserAccidents(String userId) {
        Long userIdLong = Long.parseLong(userId);
        List<EmergencyReport> reports = emergencyReportRepository.findByUserIdOrderByReportTimeDesc(userIdLong);
        
        return reports.stream()
            .map(report -> new EmergencyReportDto(
                report.getId(),
                report.getAccidentId(),
                report.getUserId(),
                report.getEmergencyNotified(),
                report.getFamilyNotified(),
                report.getReportTime()
            ))
            .toList();
    }
    
    private String formatGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "미상";
        }
        return switch (gender.toLowerCase()) {
            case "male", "m", "남성", "남" -> "남성";
            case "female", "f", "여성", "여" -> "여성";
            default -> gender;
        };
    }
    
    private String formatBloodType(String bloodType) {
        if (bloodType == null || bloodType.trim().isEmpty()) {
            return "미상";
        }
        
        String normalized = bloodType.toUpperCase().replaceAll("[^ABO]", "");
        return normalized.isEmpty() ? "미상" : normalized;
    }
}