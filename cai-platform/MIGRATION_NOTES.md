# CAI Platform — Migration Notes (Salesforce Apex → Java 21 / Spring Boot / PostgreSQL)

Greenfield migration of the legacy Salesforce (Apex) retail-banking org into a
Spring Boot 3.3 application targeting **Java 21** with a **PostgreSQL** relational
model (Flyway migrations). Lives under `cai-platform/` so the existing CI
(`mvn -B test` in `cai-platform`) exercises it.

## Package layout
```
com.cibc.cai
├── entity/        JPA entities
├── repository/    Spring Data repositories
├── service/       Business logic (ported Apex services)
├── controller/    REST endpoints
└── config/        Async / RestClient / external config
```

## Salesforce → relational model

The Salesforce **Person Account** model (a merged Account+Contact) is redesigned
as a normal relational `client` table. Money/rate fields use `BigDecimal`; dates
use `java.time`.

### Client  (from Salesforce `Account` Person Account)
| Salesforce field | Type (SF) | Java / column |
|---|---|---|
| `FirstName` | Text | `String firstName` |
| `LastName` | Text | `String lastName` |
| `SIN_Hash__pc` | Text (SHA-256 hex) | `String sinHash` — VARCHAR(64) |
| `PersonBirthdate` | Date | `LocalDate dateOfBirth` |
| `PersonEmail` | Email | `String email` |
| `Phone` | Phone | `String phone` |
| `BillingState` | Text (province) | `String province` |
| `KYC_Status__c` | Picklist | `String kycStatus` — values: `Pending`, `Verified`, `Failed`, `Error` |
| `KYC_Verified_Date__c` | Date | `LocalDate kycVerifiedDate` |

Dedupe key: (`sinHash`, `dateOfBirth`).

### Loan Application  (`Loan_Application__c`)  — field metadata (exact)
| Field | SF type | precision/scale/len | Java / column |
|---|---|---|---|
| `Amount__c` | Currency | 18,2 | `BigDecimal amount` NUMERIC(18,2) |
| `Offered_Rate__c` | Number | 18,4 | `BigDecimal offeredRate` NUMERIC(18,4) |
| `Amortization_Months__c` | Number | 4,0 | `Integer amortizationMonths` |
| `Annual_Income__c` | Currency | 18,2 | `BigDecimal annualIncome` NUMERIC(18,2) |
| `Monthly_Housing_Costs__c` | Currency | 18,2 | `BigDecimal monthlyHousingCosts` NUMERIC(18,2) |
| `Monthly_Debt_Payments__c` | Currency | 18,2 | `BigDecimal monthlyDebtPayments` NUMERIC(18,2) |
| `Status__c` | Picklist | — | `String status` — `Draft, Submitted, Approved, Declined, Funded, Cancelled` |
| `GDS_Ratio__c` | Number | 18,4 | `BigDecimal gdsRatio` NUMERIC(18,4) |
| `TDS_Ratio__c` | Number | 18,4 | `BigDecimal tdsRatio` NUMERIC(18,4) |
| `Decline_Reason__c` | Text | 255 | `String declineReason` VARCHAR(255) |
| `Submitted_Date__c` | DateTime | — | `Instant submittedDate` (TIMESTAMP) |
| `Client__c` | Lookup(Account) | — | FK `client_id` → client |

Name field: SF AutoNumber `LA-{000000}` → surrogate PK + optional `application_number`.

### Transaction Dispute  (`Transaction_Dispute__c`)  — field metadata (exact)
| Field | SF type | precision/scale/len | Java / column |
|---|---|---|---|
| `Client__c` | Lookup(Account) | — | FK `client_id` → client |
| `Card_Last4__c` | Text | 4 | `String cardLast4` VARCHAR(4) |
| `Amount__c` | Currency | 18,2 | `BigDecimal amount` NUMERIC(18,2) |
| `Reason_Code__c` | Text | 40 | `String reasonCode` VARCHAR(40) |
| `Card_Present__c` | Checkbox | default false | `boolean cardPresent` |
| `Status__c` | Picklist | — | `String status` — `Open, Escalated` |
| `Filed_Date__c` | DateTime | — | `Instant filedDate` (TIMESTAMP) |
| `SLA_Due_Date__c` | **Date** | — | see SLA decision below |
| `Escalation_Case__c` | Lookup(Case) | — | FK `escalation_case_id` → escalation_case |

Name field: SF AutoNumber `TD-{000000}`.

### EscalationCase (from Salesforce standard `Case` created in `escalate`)
Modeled as a local `escalation_case` table (fields used: `Subject`, `Priority`,
`Origin`, `AccountId`→client). No external ticketing integration is introduced;
a follow-up could swap this for a ServiceNow/Jira integration.

## Behavioral decisions (preserved vs. fixed)

### SLA date-granularity bug — **PRESERVED (default), flagged**
Apex: `dispute.SLA_Due_Date__c = Date.today().addDays(slaHours / 24);`
With `URGENT_SLA_HOURS = 48`, integer division `48 / 24 = 2`, so a "48-hour SLA"
becomes **today + 2 calendar days** on a *Date* field, not a true 48h timestamp.
Migration keeps the identical behavior (`+2 days`) so numbers match the legacy
system and the existing test (`escalatesLargeDispute`) still passes. This is
surfaced as a decision point: fixing it properly requires promoting
`SLA_Due_Date__c` to a timestamp and computing `filedDate + 48h`.

### Other preserved semantics
- GDS ≤ 0.39, TDS ≤ 0.44, stress-test buffer +0.02 on the offered rate.
- `monthlyPayment` amortization: `principal * (r*factor)/(factor-1)`, `r = annualRate/12`, `factor = (1+r)^months` — replicated with `BigDecimal` (`.pow`, `setScale`) to keep GDS/TDS identical; `stressTestedRate` for the test case = `0.0549 + 0.02 = 0.0749`.
- KYC gate: `Verified` and `kycVerifiedDate` within 365 days (inclusive), `daysBetween`-equivalent using `ChronoUnit.DAYS`.
- Loan status state machine (`ALLOWED_TRANSITIONS`) enforced at the service / `@PreUpdate` layer; `Submitted_Date__c` stamped on Draft→Submitted.
- `Database.Savepoint`/rollback → `@Transactional`.
- `@future(callout=true)` KYC callout → Spring `@Async` + reactive/`RestClient` (virtual threads for I/O).
- Dispute escalation: amount > $500 OR (fraud code `FRAUD_10.4`/`FRAUD_10.1` AND card-present) → urgent; else 10 business days via `addBusinessDays` (weekends skipped, reimplemented with `DayOfWeek`).
