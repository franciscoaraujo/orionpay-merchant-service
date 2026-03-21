package orionpay.merchant.infrastructure.adapters.output.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import orionpay.merchant.domain.model.Terminal;
import orionpay.merchant.domain.model.enums.TerminalStatus;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.TerminalMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaTerminalRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TerminalRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TerminalRepositoryAdapter implements TerminalRepository {

    private final JpaTerminalRepository jpaTerminalRepository;
    private final TerminalMapper terminalMapper;

    @Override
    public Optional<Terminal> findBySerialNumber(String serialNumber) {
        return jpaTerminalRepository.findBySerialNumber(serialNumber)
                .map(terminalMapper::toDomain);
    }

    @Override
    public List<Terminal> findAllByMerchantId(UUID merchantId) {
        return jpaTerminalRepository.findByMerchantId(merchantId).stream()
                .map(terminalMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Terminal save(Terminal terminal) {
        var entity = terminalMapper.toEntity(terminal);
        var savedEntity = jpaTerminalRepository.save(entity);
        return terminalMapper.toDomain(savedEntity);
    }

    @Override
    public Long countByMerchantIdAndStatus(UUID merchantId, String status) {
        TerminalStatus terminalStatus = TerminalStatus.valueOf(status.toUpperCase());
        return jpaTerminalRepository.countByMerchantIdAndStatus(merchantId, terminalStatus);
    }
}
