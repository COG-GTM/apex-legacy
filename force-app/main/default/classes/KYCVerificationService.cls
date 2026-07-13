/**
 * Asynchronous KYC verification against the bank's identity provider.
 * Uses @future callout since Salesforce forbids callouts in triggers/DML context.
 */
public with sharing class KYCVerificationService {

    private static final String IDENTITY_ENDPOINT = 'callout:Identity_Provider/v2/verify';

    public static void enqueueVerification(Id clientId) {
        verifyAsync(clientId);
    }

    @future(callout=true)
    private static void verifyAsync(Id clientId) {
        Account client = [
            SELECT Id, SIN_Hash__pc, PersonBirthdate, KYC_Status__c
            FROM Account WHERE Id = :clientId LIMIT 1
        ];

        HttpRequest req = new HttpRequest();
        req.setEndpoint(IDENTITY_ENDPOINT);
        req.setMethod('POST');
        req.setHeader('Content-Type', 'application/json');
        req.setBody(JSON.serialize(new Map<String, Object>{
            'sinHash' => client.SIN_Hash__pc,
            'dob' => String.valueOf(client.PersonBirthdate)
        }));

        HttpResponse res = new Http().send(req);
        if (res.getStatusCode() == 200) {
            Map<String, Object> body =
                (Map<String, Object>) JSON.deserializeUntyped(res.getBody());
            Boolean verified = (Boolean) body.get('verified');
            client.KYC_Status__c = verified ? 'Verified' : 'Failed';
            client.KYC_Verified_Date__c = verified ? Date.today() : null;
        } else {
            client.KYC_Status__c = 'Error';
        }
        update client;
    }
}
