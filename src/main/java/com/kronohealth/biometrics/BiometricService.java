package com.kronohealth.biometrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricService {

    private final BiometricRepository biometricRepository;

    @Transactional
    public BiometricEntry save(BiometricEntry entry) {
        return biometricRepository.save(entry);
    }

    @Transactional
    public List<BiometricEntry> saveAll(List<BiometricEntry> entries) {
        return biometricRepository.saveAll(entries);
    }

    public List<BiometricEntry> getByUser(UUID userId) {
        return biometricRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    public List<BiometricEntry> getByUserAndRange(UUID userId, Instant from, Instant to) {
        return biometricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, from, to);
    }

    public List<BiometricEntry> getByUserAndType(UUID userId, DataType dataType) {
        return biometricRepository.findByUserIdAndDataTypeOrderByRecordedAtDesc(userId, dataType);
    }
}
