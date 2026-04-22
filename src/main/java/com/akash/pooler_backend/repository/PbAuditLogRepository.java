package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PbAuditLogRepository extends JpaRepository<PbAuditLogEntity, Long> {
}
