package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByOwnerClientIdOrderByCreatedAtDesc(Long ownerClientId);

    boolean existsByOwnerClientIdAndPhone(Long ownerClientId, String phone);

    boolean existsByOwnerClientIdAndBeneficiaryClientId(Long ownerClientId, Long beneficiaryClientId);

    Optional<Beneficiary> findByIdAndOwnerClientId(Long id, Long ownerClientId);

    void deleteByIdAndOwnerClientId(Long id, Long ownerClientId);
}
