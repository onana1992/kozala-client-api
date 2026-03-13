package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    Optional<Client> findByPhone(String phone);

    /** Trouve les clients dont le téléphone est dans la liste (numéros normalisés). */
    List<Client> findByPhoneIn(List<String> phones);

    /**
     * Page de clients avec uniquement id, phone, displayName (pour lookup répertoire).
     * Pagination pour limiter la mémoire ; le filtrage par numéros du client se fait en mémoire par page.
     */
    @Query(
        value = "SELECT c.id as id, c.phone as phone, c.displayName as displayName FROM Client c ORDER BY c.id",
        countQuery = "SELECT COUNT(c) FROM Client c"
    )
    Page<ClientLookupProjection> findAllForLookup(Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
