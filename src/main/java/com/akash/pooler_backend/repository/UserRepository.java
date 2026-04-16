package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author AKash Kumar
 */
public interface UserRepository extends JpaRepository<PbUserEntity, Long> {
}
