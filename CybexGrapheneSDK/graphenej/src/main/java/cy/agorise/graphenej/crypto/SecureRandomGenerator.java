package cy.agorise.graphenej.crypto;

import java.security.SecureRandom;


public class SecureRandomGenerator {

    public static SecureRandom getSecureRandom(){
        SecureRandomStrengthener randomStrengthener = SecureRandomStrengthener.getInstance();
//        randomStrengthener.addEntropySource(new AndroidRandomSource());
        return randomStrengthener.generateAndSeedRandomNumberGenerator();
    }
}
