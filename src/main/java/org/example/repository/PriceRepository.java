package org.example.repository;

import org.example.entity.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<PriceEntity, Long> {
    
    /**
     * Belirli bir interval tipindeki fiyat verilerini timestamp'e göre sıralayarak getirir
     */
    List<PriceEntity> findByIntervalTypeOrderByTimestampDesc(PriceEntity.IntervalType intervalType);
    
    /**
     * Belirli bir tarih aralığındaki fiyat verilerini getirir
     */
    @Query("SELECT p FROM PriceEntity p WHERE p.intervalType = :intervalType " +
           "AND p.timestamp BETWEEN :startDate AND :endDate ORDER BY p.timestamp ASC")
    List<PriceEntity> findByIntervalTypeAndDateRange(
            @Param("intervalType") PriceEntity.IntervalType intervalType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Son N kaydı getirir (teknik analiz için)
     */
    @Query("SELECT p FROM PriceEntity p WHERE p.intervalType = :intervalType " +
           "ORDER BY p.timestamp DESC LIMIT :limit")
    List<PriceEntity> findLatestNByIntervalType(
            @Param("intervalType") PriceEntity.IntervalType intervalType,
            @Param("limit") int limit);
    
    /**
     * En son fiyat verisini getirir
     */
    @Query("SELECT p FROM PriceEntity p WHERE p.intervalType = :intervalType " +
           "ORDER BY p.timestamp DESC LIMIT 1")
    PriceEntity findLatestByIntervalType(@Param("intervalType") PriceEntity.IntervalType intervalType);
    
    /**
     * Belirli bir timestamp'teki veriyi kontrol eder
     */
    boolean existsByTimestampAndIntervalType(LocalDateTime timestamp, PriceEntity.IntervalType intervalType);
} 