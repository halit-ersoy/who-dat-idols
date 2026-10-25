package com.ses.whodatidols.util;

import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

/**
 * Utility for generating persistent VAPID (RFC 8292) EC P-256 key pairs.
 * Keys can be configured in application.properties or via environment variables:
 * VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY.
 */
public class VapidKeyGenerator {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static record VapidKeyPair(String publicKey, String privateKey) {}

    public static VapidKeyPair generateKeyPair() {
        try {
            ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
            keyPairGenerator.initialize(parameterSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            byte[] pubBytes = Utils.encode((ECPublicKey) keyPair.getPublic());
            byte[] privBytes = Utils.encode((ECPrivateKey) keyPair.getPrivate());

            String publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(pubBytes);
            String privateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(privBytes);

            return new VapidKeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("VAPID anahtar çifti oluşturulamadı: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        VapidKeyPair pair = generateKeyPair();
        System.out.println("--- YENİ PERSISTENT VAPID ANAHTARLARI ---");
        System.out.println("Public Key: " + pair.publicKey());
        System.out.println("Private Key: " + pair.privateKey());
        System.out.println("-----------------------------------------");
    }
}
