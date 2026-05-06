import java.security.*;
import java.util.Base64;
import java.nio.file.*;

public class GenKeys {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privateKey = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPrivate().getEncoded()) +
            "\n-----END PRIVATE KEY-----\n";

        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
            Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPublic().getEncoded()) +
            "\n-----END PUBLIC KEY-----\n";

        Files.writeString(Path.of(args[0]), privateKey);
        Files.writeString(Path.of(args[1]), publicKey);
        System.out.println("Keys generated successfully");
    }
}
