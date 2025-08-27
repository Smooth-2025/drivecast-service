package com.smooth.drivecast_service.emergency.service;

import com.smooth.drivecast_service.global.config.TwilioConfig;
import com.smooth.drivecast_service.emergency.dto.EmergencyUserInfoDto;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {
    
    private final TwilioConfig twilioConfig;
    
    @Value("${TWILIO_TEST_NUMBER}")
    private String testPhoneNumber;
    
    public boolean send119(String message) {
        log.info("SMS 발송 시도 - testPhoneNumber: {}", testPhoneNumber);
        try {
            Message twilioMessage = Message.creator(
                new PhoneNumber(testPhoneNumber), 
                new PhoneNumber(twilioConfig.getPhoneNumber()),
                "[Emergency 119] " + message
            ).create();
            
            log.info("119 SMS 발송 성공 - SID: {}, To: {}", twilioMessage.getSid(), testPhoneNumber);
            return true;
        } catch (Exception e) {
            log.error("119 SMS 발송 실패: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean sendFamily(EmergencyUserInfoDto userInfo, String message) {
        log.info("가족 SMS 발송 시작 - 유저ID: {}", userInfo.getUserId());
        boolean allSent = true;
        
        if (userInfo.getEmergencyContact1() != null && !userInfo.getEmergencyContact1().trim().isEmpty()) {
            // 테스트용으로 모든 가족 연락처는 testPhoneNumber로 발송
            // String convertedNumber = convertToInternationalFormat(userInfo.getEmergencyContact1());
            // allSent &= sendSingleSms(convertedNumber, "[Family Emergency 1] " + message);
            allSent &= sendSingleSms(testPhoneNumber, "[Family Emergency 1] " + message);
            log.info("가족1에게 SMS 발송 완료: {} -> {}", userInfo.getEmergencyContact1(), testPhoneNumber);
        }
        
        if (userInfo.getEmergencyContact2() != null && !userInfo.getEmergencyContact2().trim().isEmpty()) {
            // 테스트용으로 모든 가족 연락처는 testPhoneNumber로 발송
            // String convertedNumber = convertToInternationalFormat(userInfo.getEmergencyContact2());
            // allSent &= sendSingleSms(convertedNumber, "[Family Emergency 2] " + message);
            allSent &= sendSingleSms(testPhoneNumber, "[Family Emergency 2] " + message);
            log.info("가족2에게 SMS 발송 완료: {} -> {}", userInfo.getEmergencyContact2(), testPhoneNumber);
        }
        
        if (userInfo.getEmergencyContact3() != null && !userInfo.getEmergencyContact3().trim().isEmpty()) {
            // 테스트용으로 모든 가족 연락처는 testPhoneNumber로 발송
            // String convertedNumber = convertToInternationalFormat(userInfo.getEmergencyContact3());
            // allSent &= sendSingleSms(convertedNumber, "[Family Emergency 3] " + message);
            allSent &= sendSingleSms(testPhoneNumber, "[Family Emergency 3] " + message);
            log.info("가족3에게 SMS 발송 완료: {} -> {}", userInfo.getEmergencyContact3(), testPhoneNumber);
        }
        
        return allSent;
    }
    
    private boolean sendSingleSms(String to, String messageBody) {
        try {
            Message twilioMessage = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(twilioConfig.getPhoneNumber()),
                messageBody
            ).create();
            
            log.info("SMS 발송 성공 - SID: {}, To: {}", twilioMessage.getSid(), to);
            return true;
        } catch (Exception e) {
            log.error("SMS 발송 실패 - To: {}, Error: {}", to, e.getMessage());
            return false;
        }
    }
}
