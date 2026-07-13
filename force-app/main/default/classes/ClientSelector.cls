/**
 * SOQL selector layer for client (person Account) lookups.
 */
public with sharing class ClientSelector {

    public static List<Account> bySinHashAndDob(String sinHash, Date dob) {
        return [
            SELECT Id, FirstName, LastName, KYC_Status__c, KYC_Verified_Date__c
            FROM Account
            WHERE SIN_Hash__pc = :sinHash AND PersonBirthdate = :dob
            LIMIT 5
        ];
    }

    public static List<Account> withStaleKyc(Integer olderThanDays) {
        Date cutoff = Date.today().addDays(-olderThanDays);
        return [
            SELECT Id, KYC_Status__c, KYC_Verified_Date__c
            FROM Account
            WHERE KYC_Status__c = 'Verified' AND KYC_Verified_Date__c < :cutoff
            ORDER BY KYC_Verified_Date__c ASC
            LIMIT 200
        ];
    }
}
