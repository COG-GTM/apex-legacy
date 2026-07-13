# CAI Platform

Java 21 / Spring Boot 3.3 / PostgreSQL migration of the legacy Salesforce (Apex)
retail-banking org (`../force-app`). Handles client onboarding + KYC, loan/mortgage
qualification, and transaction disputes.

See [`MIGRATION_NOTES.md`](MIGRATION_NOTES.md) for the Salesforce→relational field
mapping and preserved-vs-fixed behavioral decisions.

## Build & test

```bash
cd cai-platform
mvn -B test          # unit + Testcontainers integration tests (needs Docker for IT)
mvn spring-boot:run  # requires a reachable PostgreSQL (see application.yml env vars)
```

Requires **JDK 21**. Runtime config (datasource, identity-provider base URL/auth)
is externalized in `src/main/resources/application.yml` via environment variables.
