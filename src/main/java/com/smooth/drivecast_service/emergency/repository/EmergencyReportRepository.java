package com.smooth.drivecast_service.emergency.repository;

import com.smooth.drivecast_service.emergency.entity.EmergencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyReportRepository extends JpaRepository<EmergencyReport, Long> {

    boolean existsByAccidentIdAndUserId(String accidentId, Long userId);

    List<EmergencyReport> findByUserIdOrderByReportTimeDesc(Long userId);

    Optional<EmergencyReport> findByAccidentId(String accidentId);
}
