package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.Address;
import com.neobank.kozala_client.entity.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByClientId(Long clientId);

    List<Address> findByClientIdAndType(Long clientId, AddressType type);

    Optional<Address> findByClientIdAndPrimaryAddressTrue(Long clientId);
}
