package com.smooth.drivecast_service.emergency.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_reports")
@Getter
@Setter
@NoArgsConstructor
public class EmergencyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accident_id", nullable = false)
    private String accidentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "emergency_notified", nullable = false)
    private Boolean emergencyNotified = false;

    @Column(name = "family_notified", nullable = true)
    private Boolean familyNotified = false;

    @Column(name = "report_time", nullable = false)
    private LocalDateTime reportTime;

    @Column(name = "latitude", nullable = true, precision = 8, scale = 5)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = true, precision = 8, scale = 5)
    private BigDecimal longitude;

    @PrePersist
    protected void onCreate() {
        this.reportTime = LocalDateTime.now();
    }
}