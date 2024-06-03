package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.TransactionType;
import net.luversof.api.bookkeeping.base.repository.mariadb.TransactionTypeRepository;

@RequiredArgsConstructor
@Service
public class TransactionTypeService extends AbstractBaseService<TransactionType, UUID> {

	@Getter
	private final TransactionTypeRepository repository;

	public List<TransactionType> findByLedgerId(UUID ledgerId) {
		return repository.findByLedgerId(ledgerId);
	}

}
