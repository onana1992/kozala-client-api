package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.RelatedPerson;
import com.neobank.kozala_client.entity.RelatedPersonRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelatedPersonRepository extends JpaRepository<RelatedPerson, Long> {

    List<RelatedPerson> findByClientId(Long clientId);

    List<RelatedPerson> findByClientIdAndRole(Long clientId, RelatedPersonRole role);
}
