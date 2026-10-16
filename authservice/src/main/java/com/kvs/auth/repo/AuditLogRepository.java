package com.kvs.auth.repo;

import com.kvs.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByIpAddress(String ipAddress);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:ipAddress IS NULL OR a.ipAddress = :ipAddress) AND " +
           "(:from IS NULL OR a.eventTime >= :from) AND " +
           "(:to IS NULL OR a.eventTime <= :to) " +
           "ORDER BY a.eventTime DESC")
    List<AuditLog> findByFilters(
            @Param("userId") String userId,
            @Param("ipAddress") String ipAddress,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
