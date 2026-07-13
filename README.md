# CIBC Retail Banking — Legacy Salesforce Org (Apex)

Sample of the legacy Salesforce implementation used by the retail-banking client
operations team. This org handles **client onboarding**, **loan/mortgage
applications**, and **transaction disputes** on the Salesforce platform using
Apex, SOQL, and declarative triggers.

> This repository is the *source* system for a migration off Salesforce onto
> the bank's custom platform (**CAI**). See [`docs/MIGRATION_PLAN.md`](docs/MIGRATION_PLAN.md)
> once the planning phase lands.

## What lives here

| Area | Apex artifacts | Salesforce features used |
|------|----------------|--------------------------|
| Client onboarding & KYC | `ClientOnboardingService`, `KYCVerificationService`, `ClientSelector` | SOQL on `Account`/`Contact`, DML, custom settings |
| Loan & mortgage applications | `LoanApplicationService`, `LoanApplicationTriggerHandler`, `LoanApplicationTrigger` | Custom object `Loan_Application__c`, triggers, `Database.Savepoint`, async `@future` callouts |
| Transaction disputes | `TransactionDisputeService` | Custom object `Transaction_Dispute__c`, Cases, SLA escalation |
| Tests | `LoanApplicationServiceTest`, `TransactionDisputeServiceTest` | `@isTest`, `Test.startTest/stopTest`, mock data factories |

## Layout

```
force-app/main/default/
├── classes/        Apex services, selectors, trigger handlers, tests
├── triggers/       Record triggers delegating to handlers
└── objects/        Custom object + field metadata (Loan_Application__c, Transaction_Dispute__c)
```

## Business rules encoded in Apex (must survive the migration)

1. **Loan qualification** — gross debt service (GDS) ratio must be ≤ 39% and
   total debt service (TDS) ≤ 44%; rates are stress-tested at +2% (see
   `LoanApplicationService.qualify`).
2. **KYC gating** — no loan application may move past `Submitted` unless the
   client's KYC status is `Verified` within the last 365 days.
3. **Dispute SLA** — disputes over $500 or involving card-present fraud are
   escalated to a Case with a 48-hour SLA; all others get 10 business days.
4. **Onboarding dedupe** — onboarding matches existing clients by SIN hash +
   date of birth before creating a new `Account`.
