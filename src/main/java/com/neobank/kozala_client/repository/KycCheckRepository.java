package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.KycCheck;
import com.neobank.kozala_client.entity.KycCheckResult;
import com.neobank.kozala_client.entity.KycCheckType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycCheckRepository extends JpaRepository<KycCheck, Long> {

    List<KycCheck> findByClientId(Long clientId);

    List<KycCheck> findByClientIdAndType(Long clientId, KycCheckType type);

    List<KycCheck> findByClientIdAndResult(Long clientId, KycCheckResult result);

    Optional<KycCheck> findFirstByClientIdAndTypeOrderByCheckedAtDesc(Long clientId, KycCheckType type);
}
