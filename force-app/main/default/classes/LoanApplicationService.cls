/**
 * Core loan/mortgage application logic for retail banking.
 * Encodes GDS/TDS qualification with a +2% stress test on the offered rate.
 */
public with sharing class LoanApplicationService {

    public class QualificationResult {
        public Boolean qualified;
        public Decimal gdsRatio;
        public Decimal tdsRatio;
        public Decimal stressTestedRate;
        public String declineReason;
    }

    public class LoanApplicationException extends Exception {}

    private static final Decimal MAX_GDS = 0.39;
    private static final Decimal MAX_TDS = 0.44;
    private static final Decimal STRESS_TEST_BUFFER = 0.02;

    /**
     * Qualifies an application using stress-tested payments.
     * Rule: GDS <= 39%, TDS <= 44%, rate stress-tested at offered + 2%.
     */
    public static QualificationResult qualify(Id applicationId) {
        Loan_Application__c app = [
            SELECT Id, Amount__c, Offered_Rate__c, Amortization_Months__c,
                   Annual_Income__c, Monthly_Housing_Costs__c, Monthly_Debt_Payments__c,
                   Status__c, Client__c, Client__r.KYC_Status__c, Client__r.KYC_Verified_Date__c
            FROM Loan_Application__c
            WHERE Id = :applicationId
            LIMIT 1
        ];

        assertKycCurrent(app);

        QualificationResult result = new QualificationResult();
        result.stressTestedRate = app.Offered_Rate__c + STRESS_TEST_BUFFER;

        Decimal monthlyPayment = monthlyPayment(
            app.Amount__c, result.stressTestedRate, Integer.valueOf(app.Amortization_Months__c));
        Decimal monthlyIncome = app.Annual_Income__c / 12;

        result.gdsRatio = (monthlyPayment + app.Monthly_Housing_Costs__c) / monthlyIncome;
        result.tdsRatio = (monthlyPayment + app.Monthly_Housing_Costs__c
                           + app.Monthly_Debt_Payments__c) / monthlyIncome;

        if (result.gdsRatio > MAX_GDS) {
            result.qualified = false;
            result.declineReason = 'GDS ratio ' + result.gdsRatio.setScale(4) + ' exceeds 39%';
        } else if (result.tdsRatio > MAX_TDS) {
            result.qualified = false;
            result.declineReason = 'TDS ratio ' + result.tdsRatio.setScale(4) + ' exceeds 44%';
        } else {
            result.qualified = true;
        }

        persistDecision(app, result);
        return result;
    }

    /** Standard amortized monthly payment. */
    @TestVisible
    private static Decimal monthlyPayment(Decimal principal, Decimal annualRate, Integer months) {
        Decimal r = annualRate / 12;
        Decimal factor = (1 + r).pow(months);
        return principal * (r * factor) / (factor - 1);
    }

    /** KYC gating: verified within the last 365 days or the application cannot proceed. */
    private static void assertKycCurrent(Loan_Application__c app) {
        Boolean verified = app.Client__r.KYC_Status__c == 'Verified'
            && app.Client__r.KYC_Verified_Date__c != null
            && app.Client__r.KYC_Verified_Date__c.daysBetween(Date.today()) <= 365;
        if (!verified) {
            throw new LoanApplicationException(
                'KYC not current for client ' + app.Client__c + '; application blocked');
        }
    }

    private static void persistDecision(Loan_Application__c app, QualificationResult result) {
        System.Savepoint sp = Database.setSavepoint();
        try {
            app.Status__c = result.qualified ? 'Approved' : 'Declined';
            app.GDS_Ratio__c = result.gdsRatio;
            app.TDS_Ratio__c = result.tdsRatio;
            app.Decline_Reason__c = result.declineReason;
            update app;
        } catch (DmlException e) {
            Database.rollback(sp);
            throw new LoanApplicationException('Failed to persist decision: ' + e.getMessage());
        }
    }
}
