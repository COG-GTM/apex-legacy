@isTest
private class TransactionDisputeServiceTest {

    private static Id makeClient() {
        Account client = new Account(FirstName = 'Dispute', LastName = 'Tester');
        insert client;
        return client.Id;
    }

    @isTest
    static void escalatesLargeDispute() {
        Id clientId = makeClient();

        Test.startTest();
        Transaction_Dispute__c dispute = TransactionDisputeService.fileDispute(
            clientId, '4242', 750, 'GOODS_13.1', false);
        Test.stopTest();

        Transaction_Dispute__c reloaded = [
            SELECT Status__c, Escalation_Case__c FROM Transaction_Dispute__c
            WHERE Id = :dispute.Id];
        System.assertEquals('Escalated', reloaded.Status__c);
        System.assertNotEquals(null, reloaded.Escalation_Case__c);
    }

    @isTest
    static void standardSlaForSmallDispute() {
        Id clientId = makeClient();

        Test.startTest();
        Transaction_Dispute__c dispute = TransactionDisputeService.fileDispute(
            clientId, '4242', 120, 'GOODS_13.1', false);
        Test.stopTest();

        Transaction_Dispute__c reloaded = [
            SELECT Status__c, SLA_Due_Date__c FROM Transaction_Dispute__c
            WHERE Id = :dispute.Id];
        System.assertEquals('Open', reloaded.Status__c);
        System.assert(reloaded.SLA_Due_Date__c > Date.today().addDays(9));
    }

    @isTest
    static void cardPresentFraudEscalatesRegardlessOfAmount() {
        System.assert(TransactionDisputeService.requiresUrgentEscalation(
            100, 'FRAUD_10.4', true));
        System.assert(!TransactionDisputeService.requiresUrgentEscalation(
            100, 'FRAUD_10.4', false));
    }

    @isTest
    static void rejectsNonPositiveAmount() {
        try {
            TransactionDisputeService.fileDispute(makeClient(), '4242', 0, 'X', false);
            System.assert(false, 'Expected DisputeException');
        } catch (TransactionDisputeService.DisputeException e) {
            System.assert(e.getMessage().contains('positive'));
        }
    }
}
