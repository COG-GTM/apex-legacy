/**
 * Client onboarding with dedupe by SIN hash + date of birth.
 * New clients start with KYC status 'Pending' and are queued for verification.
 */
public with sharing class ClientOnboardingService {

    public class OnboardingRequest {
        public String firstName;
        public String lastName;
        public String sinHash;      // SHA-256 of SIN, never the raw value
        public Date dateOfBirth;
        public String email;
        public String phone;
        public String province;
    }

    public class OnboardingException extends Exception {}

    /**
     * Returns the existing Account if a match is found (dedupe),
     * otherwise creates a new one and enqueues KYC verification.
     */
    public static Account onboard(OnboardingRequest req) {
        if (String.isBlank(req.sinHash) || req.dateOfBirth == null) {
            throw new OnboardingException('SIN hash and date of birth are required for dedupe');
        }

        List<Account> matches = ClientSelector.bySinHashAndDob(req.sinHash, req.dateOfBirth);
        if (!matches.isEmpty()) {
            return matches[0];
        }

        Account client = new Account(
            FirstName = req.firstName,
            LastName = req.lastName,
            SIN_Hash__pc = req.sinHash,
            PersonBirthdate = req.dateOfBirth,
            PersonEmail = req.email,
            Phone = req.phone,
            BillingState = req.province,
            KYC_Status__c = 'Pending'
        );
        insert client;

        KYCVerificationService.enqueueVerification(client.Id);
        return client;
    }
}
