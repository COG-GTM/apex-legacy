trigger LoanApplicationTrigger on Loan_Application__c (before update) {
    if (Trigger.isBefore && Trigger.isUpdate) {
        LoanApplicationTriggerHandler.beforeUpdate(Trigger.new, Trigger.oldMap);
    }
}
