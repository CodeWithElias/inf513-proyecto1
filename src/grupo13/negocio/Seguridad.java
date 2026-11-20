package grupo13.negocio;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de contraseñas con PBKDF2 (incluido en el JDK, sin librerías).
 * Formato guardado: pbkdf2$iteraciones$salBase64$hashBase64
 */
public final class Seguridad {

    private static final int ITERACIONES = 120000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Seguridad() {
    }

    public static String hashPassword(String password) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        byte[] hash = pbkdf2(password.toCharArray(), sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public static boolean verificarPassword(String password, String guardado) {
        String[] p = guardado.split("\\$");
        if (p.length != 4 || !p[0].equals("pbkdf2")) {
            return false;
        }
        byte[] sal = Base64.getDecoder().decode(p[2]);
        byte[] esperado = Base64.getDecoder().decode(p[3]);
        byte[] actual = pbkdf2(password.toCharArray(), sal, Integer.parseInt(p[1]));
        return java.security.MessageDigest.isEqual(esperado, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
