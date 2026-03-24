package orionpay.merchant.infrastructure.adapters.output;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Main {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12); // Custo 12

        String senhaPura = "password123";

        // Gera o hash (cada vez que rodar será diferente devido ao Salt aleatório)
        String hash = encoder.encode(senhaPura);
        System.out.println("Hash gerado: " + hash);

        // Verificação (matches)
        boolean isValida = encoder.matches("password123", hash);
        System.out.println("Senha correta? " + isValida);
    }
}
