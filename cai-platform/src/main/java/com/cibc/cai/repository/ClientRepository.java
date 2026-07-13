package com.cibc.cai.repository;

import com.cibc.cai.entity.Client;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Client}. The two query methods below port the SOQL in
 * the legacy {@code ClientSelector} class.
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Port of {@code ClientSelector.bySinHashAndDob}.
     * <pre>SELECT ... FROM Account WHERE SIN_Hash__pc = :sinHash
     *   AND PersonBirthdate = :dob LIMIT 5</pre>
     */
    @Query("SELECT c FROM Client c WHERE c.sinHash = :sinHash AND c.dateOfBirth = :dob")
    List<Client> bySinHashAndDob(@Param("sinHash") String sinHash,
                                 @Param("dob") LocalDate dob,
                                 Limit limit);

    default List<Client> bySinHashAndDob(String sinHash, LocalDate dob) {
        return bySinHashAndDob(sinHash, dob, Limit.of(5));
    }

    /**
     * Port of {@code ClientSelector.withStaleKyc}. Cutoff mirrors the Apex
     * {@code Date.today().addDays(-olderThanDays)}:
     * <pre>WHERE KYC_Status__c = 'Verified'
     *   AND KYC_Verified_Date__c &lt; cutoff
     *   ORDER BY KYC_Verified_Date__c ASC LIMIT 200</pre>
     */
    @Query("SELECT c FROM Client c WHERE c.kycStatus = 'Verified' "
            + "AND c.kycVerifiedDate < :cutoff ORDER BY c.kycVerifiedDate ASC")
    List<Client> withStaleKyc(@Param("cutoff") LocalDate cutoff, Limit limit);

    default List<Client> withStaleKyc(int olderThanDays) {
        return withStaleKyc(LocalDate.now().minusDays(olderThanDays), Limit.of(200));
    }
}
