package com.cai.platform.repository;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findBySinHashAndDateOfBirth(String sinHash, LocalDate dateOfBirth);

    @Query("SELECT c FROM Client c WHERE c.kycStatus = :status AND c.kycVerifiedDate < :cutoff ORDER BY c.kycVerifiedDate ASC")
    List<Client> findByKycStatusAndKycVerifiedDateBefore(
            @Param("status") KycStatus status,
            @Param("cutoff") LocalDate cutoff,
            Pageable pageable);
}
