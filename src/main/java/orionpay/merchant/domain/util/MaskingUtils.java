package orionpay.merchant.domain.util;

public class MaskingUtils {

    /**
     * Mascara um PAN (Número de Cartão) mantendo apenas os primeiros 6 e últimos 4 dígitos.
     * PCI DSS permite exibir BIN (primeiros 6) e Last4.
     * Exemplo: 1234567812345678 -> 123456******5678
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "******";
        }
        
        int length = pan.length();
        String bin = pan.substring(0, 6);
        String lastFour = pan.substring(length - 4);
        
        return bin + "******" + lastFour;
    }
}