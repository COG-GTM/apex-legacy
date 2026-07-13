/**
 * Trigger handler for Loan_Application__c.
 * Enforces status transitions and stamps submission metadata.
 */
public with sharing class LoanApplicationTriggerHandler {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS =
        new Map<String, Set<String>>{
            'Draft'     => new Set<String>{'Submitted', 'Cancelled'},
            'Submitted' => new Set<String>{'Approved', 'Declined', 'Cancelled'},
            'Approved'  => new Set<String>{'Funded', 'Cancelled'},
            'Declined'  => new Set<String>{},
            'Funded'    => new Set<String>{},
            'Cancelled' => new Set<String>{}
        };

    public static void beforeUpdate(
            List<Loan_Application__c> newList, Map<Id, Loan_Application__c> oldMap) {
        for (Loan_Application__c app : newList) {
            Loan_Application__c old = oldMap.get(app.Id);
            if (app.Status__c != old.Status__c) {
                validateTransition(app, old.Status__c, app.Status__c);
            }
            if (app.Status__c == 'Submitted' && old.Status__c == 'Draft') {
                app.Submitted_Date__c = Datetime.now();
            }
        }
    }

    private static void validateTransition(
            Loan_Application__c app, String fromStatus, String toStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(toStatus)) {
            app.addError('Illegal status transition: ' + fromStatus + ' → ' + toStatus);
        }
    }
}
