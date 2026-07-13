package com.cibc.cai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cibc.cai.entity.Client;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the persistence layer against a real PostgreSQL (Testcontainers) using
 * the production Flyway migration. With {@code ddl-auto=validate} this proves
 * the JPA entity mappings validate cleanly against {@code V1__init_schema.sql},
 * and exercises the two ported {@code ClientSelector} queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ClientRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void bySinHashAndDob_matchesOnSinHashAndBirthdate() {
        LocalDate dob = LocalDate.of(1985, 3, 14);
        client("Ada", "Lovelace", "hash-a", dob);
        client("Alan", "Turing", "hash-a", LocalDate.of(1912, 6, 23)); // same hash, different dob
        client("Grace", "Hopper", "hash-b", dob);                       // same dob, different hash

        List<Client> results = clientRepository.bySinHashAndDob("hash-a", dob);

        assertThat(results).extracting(Client::getFirstName).containsExactly("Ada");
    }

    @Test
    void withStaleKyc_returnsVerifiedClientsOlderThanCutoffOrderedAsc() {
        Client stale = client("Old", "Verified", "hash-c", LocalDate.of(1970, 1, 1));
        stale.setKycStatus("Verified");
        stale.setKycVerifiedDate(LocalDate.now().minusDays(400));

        Client fresh = client("New", "Verified", "hash-d", LocalDate.of(1971, 1, 1));
        fresh.setKycStatus("Verified");
        fresh.setKycVerifiedDate(LocalDate.now().minusDays(10));

        Client pending = client("Pending", "Client", "hash-e", LocalDate.of(1972, 1, 1));
        pending.setKycStatus("Pending");
        pending.setKycVerifiedDate(LocalDate.now().minusDays(500));

        clientRepository.saveAll(List.of(stale, fresh, pending));
        clientRepository.flush();

        List<Client> results = clientRepository.withStaleKyc(365);

        assertThat(results).extracting(Client::getFirstName).containsExactly("Old");
    }

    private Client client(String first, String last, String sinHash, LocalDate dob) {
        Client c = new Client();
        c.setFirstName(first);
        c.setLastName(last);
        c.setSinHash(sinHash);
        c.setDateOfBirth(dob);
        return clientRepository.save(c);
    }
}
