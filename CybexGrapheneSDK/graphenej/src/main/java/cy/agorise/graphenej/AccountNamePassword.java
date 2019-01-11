package cy.agorise.graphenej;

import org.bitcoinj.core.ECKey;
import org.spongycastle.crypto.digests.SHA256Digest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AccountNamePassword {

    private ECKey mPrivateKey;

    public AccountNamePassword(String seed) {
        try {
            SHA256Digest digest = new SHA256Digest();
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] seedBytes = seed.getBytes(Charset.forName("UTF-8"));
            digest.update(seedBytes, 0, seedBytes.length);
            byte[] bytes = md.digest(seed.getBytes("UTF-8"));
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] result = sha256.digest(bytes);
            byte[] result1 = new byte[32];
            digest.doFinal(result1, 0);
            mPrivateKey = ECKey.fromPrivate(result1);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgotithmException. Msg: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.out.println("UnsupportedEncodingException. Msg: " + e.getMessage());
        }
    }

    public ECKey getPrivateKey() {
        return mPrivateKey;
    }

    public byte[] getPublicKey() {
        return mPrivateKey.getPubKey();
    }
}
