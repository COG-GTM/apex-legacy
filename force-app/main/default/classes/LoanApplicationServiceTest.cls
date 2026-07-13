@isTest
private class LoanApplicationServiceTest {

    private static Account makeVerifiedClient() {
        Account client = new Account(
            FirstName = 'Test', LastName = 'Client',
            KYC_Status__c = 'Verified',
            KYC_Verified_Date__c = Date.today().addDays(-30)
        );
        insert client;
        return client;
    }

    private static Loan_Application__c makeApp(Id clientId, Decimal amount, Decimal income) {
        Loan_Application__c app = new Loan_Application__c(
            Client__c = clientId,
            Amount__c = amount,
            Offered_Rate__c = 0.0549,
            Amortization_Months__c = 300,
            Annual_Income__c = income,
            Monthly_Housing_Costs__c = 450,
            Monthly_Debt_Payments__c = 300,
            Status__c = 'Submitted'
        );
        insert app;
        return app;
    }

    @isTest
    static void qualifiesHealthyApplication() {
        Account client = makeVerifiedClient();
        Loan_Application__c app = makeApp(client.Id, 300000, 160000);

        Test.startTest();
        LoanApplicationService.QualificationResult result =
            LoanApplicationService.qualify(app.Id);
        Test.stopTest();

        System.assert(result.qualified, 'Healthy application should qualify');
        System.assertEquals(0.0749, result.stressTestedRate, 'Rate stress-tested at +2%');
        Loan_Application__c reloaded = [
            SELECT Status__c FROM Loan_Application__c WHERE Id = :app.Id];
        System.assertEquals('Approved', reloaded.Status__c);
    }

    @isTest
    static void declinesWhenGdsTooHigh() {
        Account client = makeVerifiedClient();
        Loan_Application__c app = makeApp(client.Id, 900000, 60000);

        Test.startTest();
        LoanApplicationService.QualificationResult result =
            LoanApplicationService.qualify(app.Id);
        Test.stopTest();

        System.assert(!result.qualified);
        System.assert(result.declineReason.contains('GDS'));
    }

    @isTest
    static void blocksStaleKyc() {
        Account client = new Account(
            FirstName = 'Stale', LastName = 'Kyc',
            KYC_Status__c = 'Verified',
            KYC_Verified_Date__c = Date.today().addDays(-400)
        );
        insert client;
        Loan_Application__c app = makeApp(client.Id, 300000, 160000);

        try {
            LoanApplicationService.qualify(app.Id);
            System.assert(false, 'Expected LoanApplicationException');
        } catch (LoanApplicationService.LoanApplicationException e) {
            System.assert(e.getMessage().contains('KYC not current'));
        }
    }
}
