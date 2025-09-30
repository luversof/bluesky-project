package net.luversof.api.stock.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Setter;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;

@Service
public class AccountService {

	@Setter(onMethod_ = @Autowired)
	private AccountRepository accountRepository;
	
	@Setter(onMethod_ = @Autowired)
	private DividendRepository dividendRepository;
	
	@Setter(onMethod_ = @Autowired)
	private TradeRepository tradeRepository;
	
	public Account createAccount(Account account) {
		return accountRepository.save(account);
	}
	
	public Optional<Account> findById(UUID id) {
		return accountRepository.findById(id);
	}
	
	public Iterable<Account> findByUserId(UUID userId) {
		return accountRepository.findByUserId(userId);
	}
	
	/**
	 * UserId 기준 데이터 일괄 삭제
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
