package orionpay.merchant.infrastructure.adapters.output.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VisaGatewayClientService {

    private static final Logger logger = LoggerFactory.getLogger(VisaGatewayClientService.class);

    @Value("${gateway.host:localhost}")
    private String gatewayHost;

    @Value("${gateway.port:8889}")
    private int gatewayPort;

    @Value("${gateway.timeout:5000}")
    private int socketTimeout;

    private static final AtomicInteger stanGenerator = new AtomicInteger(1);

    public String authorizeTransaction(String pan, BigDecimal amount, String terminalId, String merchantId) {
        String stan = generateStan();
        byte[] requestPayload = buildIso8583Request(pan, amount, terminalId, merchantId, stan);

        // Log detalhado do payload
        StringBuilder hexPayload = new StringBuilder();
        for (byte b : requestPayload) {
            hexPayload.append(String.format("%02X ", b));
        }
        logger.info(">>> ISO 8583 Request Payload (STAN: {}): {}", stan, hexPayload.toString().trim());

        try (Socket socket = new Socket(gatewayHost, gatewayPort)) {
            socket.setSoTimeout(socketTimeout);
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeShort(requestPayload.length);
            out.write(requestPayload);
            out.flush();

            int responseLength = in.readUnsignedShort();
            byte[] responsePayload = new byte[responseLength];
            in.readFully(responsePayload);
            
            // Log da resposta
            StringBuilder hexResponse = new StringBuilder();
            for (byte b : responsePayload) {
                hexResponse.append(String.format("%02X ", b));
            }
            logger.info("<<< ISO 8583 Response Payload (Length: {}): {}", responseLength, hexResponse.toString().trim());
            
            return parseIso8583Response(responsePayload);

        } catch (IOException e) {
            logger.error("Error communicating with Gateway", e);
            return "ERROR";
        }
    }

    private String generateStan() {
        int currentStan = stanGenerator.getAndIncrement() % 1000000;
        return String.format("%06d", currentStan);
    }

    private byte[] buildIso8583Request(String pan, BigDecimal amount, String terminalId, String merchantId, String stan) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(IsoUtils.stringToBcd("0100"));
            out.write(IsoUtils.hexStringToByteArray("7220000000C08000"));
            String panLengthStr = String.format("%02d", pan.length());
            out.write(IsoUtils.stringToBcd(panLengthStr));
            out.write(IsoUtils.stringToBcd(pan));
            out.write(IsoUtils.stringToBcd("000000"));
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
            String amountStr = IsoUtils.padLeftZeros(String.valueOf(amountInCents), 12);
            out.write(IsoUtils.stringToBcd(amountStr));
            LocalDateTime now = LocalDateTime.now();
            String dateTimeStr = now.format(DateTimeFormatter.ofPattern("MMddHHmmss"));
            out.write(IsoUtils.stringToBcd(dateTimeStr));
            out.write(IsoUtils.stringToBcd(stan));
            String paddedTerminalId = IsoUtils.padRightSpaces(terminalId, 8).substring(0, 8);
            out.write(IsoUtils.stringToAsciiBytes(paddedTerminalId));
            String paddedMerchantId = IsoUtils.padRightSpaces(merchantId, 15).substring(0, 15);
            out.write(IsoUtils.stringToAsciiBytes(paddedMerchantId));
            out.write(IsoUtils.stringToAsciiBytes("986"));
        } catch (IOException e) {
            throw new RuntimeException("Error building ISO 8583 request", e);
        }
        return out.toByteArray();
    }

    private String parseIso8583Response(byte[] responsePayload) {
        try {
            int offset = 0;
            byte[] mtiBytes = new byte[2];
            System.arraycopy(responsePayload, offset, mtiBytes, 0, 2);
            String mti = IsoUtils.bcdToString(mtiBytes);
            offset += 2;
            
            if (!"0110".equals(mti)) {
                logger.warn("Unexpected MTI in response: {}", mti);
                return "UNEXPECTED_RESPONSE";
            }

            byte[] bitmapBytes = new byte[8];
            System.arraycopy(responsePayload, offset, bitmapBytes, 0, 8);
            offset += 8;
            
            boolean hasDe39 = (bitmapBytes[4] & 0x02) != 0;
            
            if ((bitmapBytes[0] & 0x40) != 0) {
                byte llvarLenByte = responsePayload[offset++];
                int panBcdLen = Integer.parseInt(IsoUtils.bcdToString(new byte[]{llvarLenByte}));
                int panBytesLen = (panBcdLen + 1) / 2;
                offset += panBytesLen;
            }
            
            if ((bitmapBytes[0] & 0x20) != 0) offset += 3;
            if ((bitmapBytes[0] & 0x10) != 0) offset += 6;
            if ((bitmapBytes[0] & 0x02) != 0) offset += 5;
            if ((bitmapBytes[1] & 0x20) != 0) offset += 3;
            
            if (hasDe39) {
                byte[] de39Bytes = new byte[2];
                System.arraycopy(responsePayload, offset, de39Bytes, 0, 2);
                String de39 = new String(de39Bytes, StandardCharsets.US_ASCII);
                offset += 2;
                
                switch (de39) {
                    case "00": return "APPROVED";
                    case "12": return "INVALID_TRANSACTION";
                    case "51": return "INSUFFICIENT_FUNDS";
                    case "14": return "INVALID_CARD";
                    case "55": return "INCORRECT_PIN";
                    case "91": return "ISSUER_INOPERATIVE";
                    default: return "UNKNOWN_ERROR_" + de39;
                }
            } else {
                 logger.warn("DE39 is missing in response bitmap");
                 return "UNKNOWN";
            }

        } catch (Exception e) {
             logger.error("Error parsing response", e);
             return "PARSE_ERROR";
        }
    }
}
