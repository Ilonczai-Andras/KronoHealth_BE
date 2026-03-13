package com.kronohealth.biometrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BiometricRepository extends JpaRepository<BiometricEntry, UUID> {

    List<BiometricEntry> findByUserIdOrderByRecordedAtDesc(UUID userId);

    List<BiometricEntry> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID userId, Instant from, Instant to);

    List<BiometricEntry> findByUserIdAndDataTypeOrderByRecordedAtDesc(UUID userId, DataType dataType);

    /**
     * Returns daily averages per dataType for a user within a date range.
     * Result projection: [date string, dataType string, average value]
     */
    @Query(value = """
            SELECT CAST(DATE(recorded_at) AS VARCHAR) as date,
                   data_type,
                   AVG(value) as avg_value
            FROM biometric_entries
            WHERE user_id = :userId
              AND recorded_at BETWEEN :from AND :to
            GROUP BY DATE(recorded_at), data_type
            ORDER BY DATE(recorded_at) ASC, data_type ASC
            """, nativeQuery = true)
    List<Object[]> findDailyAverages(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}

