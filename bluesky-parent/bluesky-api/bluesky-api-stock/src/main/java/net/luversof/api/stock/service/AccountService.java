package net.luversof.api.stock.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;

@Service
public class AccountService {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private DividendRepository dividendRepository;

	@Autowired
	private TradeRepository tradeRepository;

	public void setAccountRepository(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public void setDividendRepository(DividendRepository dividendRepository) {
		this.dividendRepository = dividendRepository;
	}

	public void setTradeRepository(TradeRepository tradeRepository) {
		this.tradeRepository = tradeRepository;
	}

	public Account createAccount(Account account) {
		return accountRepository.save(account);
	}

	public Optional<Account> findById(UUID id) {
		return accountRepository.findById(id);
	}

	public List<Account> findByIdIn(List<UUID> idList) {
		return accountRepository.findByIdIn(idList);
	}

	public List<Account> findByUserId(UUID userId) {
		return accountRepository.findByUserId(userId);
	}

	/**
	 * UserId 기준 데이터 일괄 삭제
	 * 
	 * @param userId
	 */
	@Transactional
	public void deleteAllByUserId(UUID userId) {
		var accountList = accountRepository.findByUserId(userId);
		accountList.forEach(account -> {
			dividendRepository.deleteByAccountId(account.getId());
			tradeRepository.deleteByAccountId(account.getId());
		});
		accountRepository.deleteAll(accountList);
	}
}
