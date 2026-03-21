package orionpay.merchant.infrastructure.adapters.output.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;

import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailAdapter implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendTransactionReceipt(String to, ExtratoTransactionDetail receipt) {
        try {
            // Usamos MimeMessage para suportar HTML
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Comprovante de Transação - OrionPay");

            // Formatação amigável para Moeda e Data
            String valorFormatado = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                    .format(receipt.getAmount());
            String dataFormatada = receipt.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            // Template HTML inline inspirado no layout do Figma
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; background-color: #f8fafc; padding: 40px 20px;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #0f172a; margin: 0; font-size: 20px;">OrionPay</h2>
                        <p style="color: #64748b; margin: 4px 0 0 0; font-size: 14px;">Comprovante de transação</p>
                    </div>
                    
                    <div style="background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; max-width: 600px; margin: 0 auto; padding: 32px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                        
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 24px;">
                            <tr>
                                <td align="left">
                                    <p style="font-size: 12px; color: #64748b; font-weight: bold; margin:0 0 4px 0; text-transform: uppercase;">Transação</p>
                                    <p style="font-size: 24px; color: #0f172a; font-weight: bold; margin:0;">%s</p>
                                </td>
                                <td align="right">
                                    <p style="font-size: 12px; color: #64748b; font-weight: bold; margin:0 0 4px 0; text-transform: uppercase;">Valor</p>
                                    <p style="font-size: 24px; color: #0f172a; font-weight: bold; margin:0;">%s</p>
                                </td>
                            </tr>
                        </table>
                        
                        <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 24px 0;">
                        
                        <table width="100%%" cellpadding="12" cellspacing="12" style="background-color: #ffffff; border-collapse: separate; border-spacing: 12px; margin: -12px;">
                            <tr>
                                <td width="50%%" style="border: 1px solid #e2e8f0; border-radius: 8px;">
                                    <p style="font-size: 11px; color: #64748b; font-weight: bold; margin:0 0 4px 0; text-transform: uppercase;">NSU</p>
                                    <p style="font-size: 14px; color: #0f172a; font-weight: bold; margin:0;">**** **** **** %s</p>
                                </td>
                                <td width="50%%" style="border: 1px solid #e2e8f0; border-radius: 8px;">
                                    <p style="font-size: 11px; color: #64748b; font-weight: bold; margin:0 0 4px 0; text-transform: uppercase;">Data/Hora</p>
                                    <p style="font-size: 14px; color: #0f172a; font-weight: bold; margin:0;">%s</p>
                                </td>
                            </tr>
                            <tr>
                                <td width="50%%" style="border: 1px solid #e2e8f0; border-radius: 8px;">
                                    <p style="font-size: 11px; color: #64748b; font-weight: bold; margin:0 0 4px 0; text-transform: uppercase;">Status</p>
                                    <p style="font-size: 14px; color: #0f172a; font-weight: bold; margin:0;">%s</p>
                                </td>
                                <td width="50%%" style="border: 1px solid #e2e8f0; border-radius: 8px;">
                                    <p style="font-size: 11px; color: #64748b; font-weight: bold; margin:0 0 4px 0; text-transform: uppercase;">Bandeira / Cartão</p>
                                    <p style="font-size: 14px; color: #0f172a; font-weight: bold; margin:0;">%s **** %s</p>
                                </td>
                            </tr>
                        </table>
                        
                        <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 24px 0;">
                        
                        <p style="font-size: 12px; color: #64748b; margin:0;">ID interno: %s</p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 24px; max-width: 600px; margin-left: auto; margin-right: auto;">
                        <p style="font-size: 11px; color: #94a3b8; line-height: 1.5;">Este comprovante é gerado para fins de conferência. Em caso de divergência, consulte o suporte OrionPay.</p>
                    </div>
                </div>
                """,
                    receipt.getExternalId(), // tx_944925
                    valorFormatado,          // R$ 499,64
                    receipt.getNsu(),        // 944925
                    dataFormatada,           // 19/03/2026 22:28
                    receipt.getStatus(),     // Negado
                    receipt.getBrand(),      // ELO
                    receipt.getLastFour(),   // 5164
                    receipt.getId().toString()
            );

            // Passa 'true' no segundo parâmetro para o JavaMail entender que é HTML
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("E-mail de comprovante enviado com sucesso para: {}", to);

        } catch (MessagingException e) {
            log.error("Erro ao montar o e-mail HTML", e);
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }
}
