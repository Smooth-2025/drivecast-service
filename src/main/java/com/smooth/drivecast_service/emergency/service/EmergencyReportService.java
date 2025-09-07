package com.smooth.drivecast_service.emergency.service;

import com.smooth.drivecast_service.emergency.feign.UserServiceClient;
import com.smooth.drivecast_service.emergency.feign.dto.EmergencyInfoResponse;
import com.smooth.drivecast_service.emergency.service.mapper.EmergencyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import com.smooth.drivecast_service.emergency.dto.AccidentInfoDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyReportResult;
import com.smooth.drivecast_service.emergency.dto.EmergencyRequestDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyResponseDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyUserInfoDto;
import com.smooth.drivecast_service.emergency.entity.EmergencyReport;
import com.smooth.drivecast_service.emergency.exception.EmergencyErrorCode;
import com.smooth.drivecast_service.emergency.feign.dto.EmergencyReportDto;
import com.smooth.drivecast_service.emergency.repository.EmergencyReportRepository;
import com.smooth.drivecast_service.global.common.cache.DedupService;
import com.smooth.drivecast_service.global.exception.BusinessException;

import com.smooth.drivecast_service.incident.dto.IncidentEvent;

import java.util.List;
import java.util.Optional;

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

    public EmergencyReportResult processEmergencyDecision(EmergencyRequestDto req, String jwtUserId) {
        final String accidentId = req.getAccidentId();
        final Long userId = jwtUserId != null ? Long.parseLong(jwtUserId) : req.getUserId();

        log.info("응급 신고 여부 판단 : accidentId = {}, userId = {}, JWT userId = {}, 신고 여부 = {}, 타임 아웃 = {} ", accidentId, userId, jwtUserId, req.getChooseReport(), req.isTimeout());

        try {
            if (isAlreadyReported(accidentId, userId)) {
                throw new BusinessException(EmergencyErrorCode.EMERGENCY_REPORT_ALREADY_EXISTS);
            }

            EmergencyReport report = createEmergencyReport(req, userId);

            if (!isEmergencyReportRequested(req)) {
                return EmergencyReportResult.cancelled(accidentId, "신고가 취소되었습니다.");
            }

            AccidentInfoDto accidentInfo = getAccidentInfo(accidentId);
            if (accidentInfo == null) {
                throw new BusinessException(EmergencyErrorCode.EMERGENCY_ACCIDENT_INFO_NOT_FOUND);
            }
            EmergencyUserInfoDto userInfo = getUserInfo(report.getUserId());
            log.info("2단계 완료: 사용자 정보 조회 성공 - userId={}, gender={}, bloodType={}, emergencyContact1={}", 
                report.getUserId(), userInfo.getGender(), userInfo.getBloodType(), userInfo.getEmergencyContact1());

            return executeEmergencyNotifications(report, accidentInfo, userInfo);

        } catch (BusinessException businessException) {
            throw businessException;
        } catch (Exception e) {
            log.error("응급 신고 처리 중 에러 발생", e);
            throw new BusinessException(EmergencyErrorCode.EMERGENCY_REPORT_PROCESSING_FAILED, e.getMessage(), e);
        }
    }

    @Transactional
    protected EmergencyReport createEmergencyReport(EmergencyRequestDto req, Long userId) {
        EmergencyReport report = new EmergencyReport();
        report.setAccidentId(req.getAccidentId());
        report.setUserId(userId);
        
        report.setEmergencyNotified(false);
        
        if (isEmergencyReportRequested(req)) {
            try {
                AccidentInfoDto accidentInfo = getAccidentInfo(req.getAccidentId());
                if (accidentInfo != null) {
                    report.setLatitude(accidentInfo.getLatitude() != null ? BigDecimal.valueOf(accidentInfo.getLatitude()) : null);
                    report.setLongitude(accidentInfo.getLongitude() != null ? BigDecimal.valueOf(accidentInfo.getLongitude()) : null);
                }
            } catch (Exception e) {
                log.warn("사고 정보 조회 실패 - 위치 정보 없이 저장: accidentId={}", req.getAccidentId());
            }
        }
        
        return emergencyReportRepository.save(report);
    }

    @Transactional
    protected EmergencyReportResult executeEmergencyNotifications(EmergencyReport report, AccidentInfoDto accidentInfo, EmergencyUserInfoDto userInfo) {
        log.info("3단계: 알림 발송 및 완료 처리 시작");

        sendEmergencySms(accidentInfo, userInfo);
        
        report.setEmergencyNotified(true);
        log.info("119 문자 발송 성공 - emergency_notified를 true로 업데이트");
        
        boolean familyNotified = sendFamilyNotification(userInfo, accidentInfo);
        report.setFamilyNotified(familyNotified);
        
        emergencyReportRepository.save(report);

        return EmergencyReportResult.reported(report.getAccidentId(), "119 신고가 접수되었습니다.");
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
                .existsByAccidentIdAndUserId(accidentId, userId);
        log.debug("중복 신고 체크: accidentId={}, userId={}, exists={}", accidentId, userId, exists);
        return exists;
    }

    private boolean isEmergencyReportRequested(EmergencyRequestDto req) {
        return req.getChooseReport() || req.isTimeout();
    }

    
    private EmergencyUserInfoDto getUserInfo(Long userId) {
        try {
            log.info("사용자 정보 조회 시도: userId={}", userId);
            EmergencyInfoResponse response = userServiceClient.getUserInfo(String.valueOf(userId));
            log.info("OpenFeign 응답: code={}, message={}", response.getCode(), response.getMessage());
            
            if (response.getData() != null) {
                log.info("응답 데이터: gender={}, bloodType={}, contact1={}", 
                    response.getData().getGender(), 
                    response.getData().getBloodType(),
                    response.getData().getEmergencyContact1());
            } else {
                log.warn("응답 데이터가 null입니다.");
            }
            
            EmergencyUserInfoDto result = emergencyMapper.toUserInfoDto(response.getData());
            log.info("매핑 결과: gender={}, bloodType={}, contact1={}", 
                result.getGender(), result.getBloodType(), result.getEmergencyContact1());
                
            return result;
        } catch (Exception e) {
            log.error("유저 정보 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
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
                report.getReportTime(),
                report.getLatitude(),
                report.getLongitude()
            ))
            .toList();
    }

    public EmergencyResponseDto getAccidentById(String accidentId) {
        Optional<EmergencyReport> reportOpt = emergencyReportRepository.findByAccidentId(accidentId);
        if (reportOpt.isEmpty()) {
            return null;
        }
        
        EmergencyReport report = reportOpt.get();
        return new EmergencyResponseDto(
            report.getEmergencyNotified(),
            report.getFamilyNotified(),
            report.getReportTime()
        );
    }
    
    
    private String formatGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "미상";
        }
        return switch (gender.toUpperCase()) {
            case "MALE", "M", "남성", "남" -> "남성";
            case "FEMALE", "F", "여성", "여" -> "여성";
            default -> "미상";
        };
    }
    
    private String formatBloodType(String bloodType) {
        if (bloodType == null || bloodType.trim().isEmpty()) {
            return "미상";
        }
        
        String normalized = bloodType.toUpperCase().replaceAll("[^ABO+\\-]", "");
        return normalized.isEmpty() ? "미상" : normalized;
    }
}