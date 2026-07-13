CREATE TABLE client (
    id                  BIGSERIAL PRIMARY KEY,
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    sin_hash            VARCHAR(64),
    date_of_birth       DATE,
    email               VARCHAR(255),
    phone               VARCHAR(50),
    province            VARCHAR(50),
    kyc_status          VARCHAR(20),
    kyc_verified_date   DATE
);

CREATE UNIQUE INDEX idx_client_sin_hash_dob ON client (sin_hash, date_of_birth);
CREATE INDEX idx_client_kyc_status_verified_date ON client (kyc_status, kyc_verified_date);

CREATE TABLE loan_application (
    id                      BIGSERIAL PRIMARY KEY,
    client_id               BIGINT NOT NULL REFERENCES client (id),
    amount                  NUMERIC(14, 2),
    offered_rate            NUMERIC(7, 5),
    amortization_months     INTEGER,
    annual_income           NUMERIC(14, 2),
    monthly_housing_costs   NUMERIC(12, 2),
    monthly_debt_payments   NUMERIC(12, 2),
    status                  VARCHAR(20),
    gds_ratio               NUMERIC(8, 5),
    tds_ratio               NUMERIC(8, 5),
    decline_reason          VARCHAR(255),
    submitted_date          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_loan_application_client ON loan_application (client_id);

CREATE TABLE escalation_case (
    id              BIGSERIAL PRIMARY KEY,
    subject         VARCHAR(255),
    priority        VARCHAR(20),
    origin          VARCHAR(50),
    client_id       BIGINT REFERENCES client (id),
    created_date    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE transaction_dispute (
    id                  BIGSERIAL PRIMARY KEY,
    client_id           BIGINT NOT NULL REFERENCES client (id),
    card_last4          VARCHAR(4),
    amount              NUMERIC(14, 2),
    reason_code         VARCHAR(50),
    card_present        BOOLEAN,
    status              VARCHAR(20),
    filed_date          TIMESTAMP WITH TIME ZONE,
    sla_due_date        DATE,
    escalation_case_id  BIGINT REFERENCES escalation_case (id)
);

CREATE INDEX idx_transaction_dispute_client ON transaction_dispute (client_id);
