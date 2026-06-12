package orionpay.merchant.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.EncryptionException;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    /**
     * Chave privada RSA codificada em Base64 (PKCS#8).
     * Em produção, forneça via variável de ambiente / Key Vault e NUNCA commite a chave real.
     * Exemplo no application.yml:
     *   encryption.rsa.private-key: ${RSA_PRIVATE_KEY}
     */
    @Value("${encryption.rsa.private-key:}")
    private String privateKeyBase64;

    /**
     * Descriptografa um valor cifrado com a chave pública RSA correspondente.
     *
     * @param encryptedData dado cifrado em Base64
     * @return texto plano descriptografado
     * @throws EncryptionException se o dado for inválido, a chave estiver errada
     *                             ou ocorrer qualquer falha criptográfica
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isBlank()) {
            return encryptedData;
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());

            byte[] cipherBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            // Dado de entrada não é Base64 válido
            log.warn("Dado recebido não está em formato Base64 válido.");
            throw new EncryptionException("O dado fornecido não possui codificação Base64 válida.", e);

        } catch (Exception e) {
            // Falha criptográfica: chave incorreta, padding inválido, dado corrompido, etc.
            log.error("Falha ao descriptografar dado RSA: {}", e.getMessage());
            throw new EncryptionException(
                    "Falha na descriptografia RSA. Verifique se o dado está corretamente cifrado " +
                    "e se a chave privada configurada é a correspondente à chave pública utilizada.", e);
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------------------

    private PrivateKey getPrivateKey() {
        if (privateKeyBase64 == null || privateKeyBase64.isBlank()) {
            throw new EncryptionException(
                    "Chave privada RSA não configurada. " +
                    "Defina a propriedade 'encryption.rsa.private-key' ou a variável de ambiente RSA_PRIVATE_KEY.");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("A chave privada RSA não está em formato Base64 válido.", e);
        } catch (Exception e) {
            throw new EncryptionException(
                    "Não foi possível carregar a chave privada RSA. " +
                    "Certifique-se de que ela está no formato PKCS#8 codificado em Base64.", e);
        }
    }
}
