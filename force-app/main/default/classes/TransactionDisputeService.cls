/**
 * Transaction dispute intake and SLA escalation.
 * Disputes > $500 or card-present fraud escalate to a Case with a 48h SLA;
 * everything else gets a 10-business-day SLA.
 */
public with sharing class TransactionDisputeService {

    public class DisputeException extends Exception {}

    private static final Decimal ESCALATION_AMOUNT = 500;
    private static final Integer URGENT_SLA_HOURS = 48;
    private static final Integer STANDARD_SLA_BUSINESS_DAYS = 10;

    public static Transaction_Dispute__c fileDispute(
            Id clientId, String cardLast4, Decimal amount,
            String reasonCode, Boolean cardPresent) {

        if (amount == null || amount <= 0) {
            throw new DisputeException('Dispute amount must be positive');
        }

        Transaction_Dispute__c dispute = new Transaction_Dispute__c(
            Client__c = clientId,
            Card_Last4__c = cardLast4,
            Amount__c = amount,
            Reason_Code__c = reasonCode,
            Card_Present__c = cardPresent,
            Status__c = 'Open',
            Filed_Date__c = Datetime.now()
        );
        insert dispute;

        if (requiresUrgentEscalation(amount, reasonCode, cardPresent)) {
            escalate(dispute, URGENT_SLA_HOURS);
        } else {
            dispute.SLA_Due_Date__c = addBusinessDays(Date.today(), STANDARD_SLA_BUSINESS_DAYS);
            update dispute;
        }
        return dispute;
    }

    @TestVisible
    private static Boolean requiresUrgentEscalation(Decimal amount, String reasonCode, Boolean cardPresent) {
        Boolean fraudCode = reasonCode == 'FRAUD_10.4' || reasonCode == 'FRAUD_10.1';
        return amount > ESCALATION_AMOUNT || (fraudCode && cardPresent);
    }

    private static void escalate(Transaction_Dispute__c dispute, Integer slaHours) {
        Case escalation = new Case(
            Subject = 'URGENT dispute ' + dispute.Id + ' — $' + dispute.Amount__c,
            Priority = 'High',
            Origin = 'Dispute Intake',
            AccountId = dispute.Client__c
        );
        insert escalation;

        dispute.Status__c = 'Escalated';
        dispute.Escalation_Case__c = escalation.Id;
        dispute.SLA_Due_Date__c = Date.today().addDays(slaHours / 24);
        update dispute;
    }

    @TestVisible
    private static Date addBusinessDays(Date start, Integer businessDays) {
        Date result = start;
        Integer added = 0;
        while (added < businessDays) {
            result = result.addDays(1);
            Datetime dt = Datetime.newInstance(result, Time.newInstance(0, 0, 0, 0));
            String dayOfWeek = dt.format('E');
            if (dayOfWeek != 'Sat' && dayOfWeek != 'Sun') {
                added++;
            }
        }
        return result;
    }
}
