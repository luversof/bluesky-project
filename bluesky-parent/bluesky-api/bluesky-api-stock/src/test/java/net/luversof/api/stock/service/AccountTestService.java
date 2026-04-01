package net.luversof.api.stock.service;

import java.util.HashMap;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountTestService extends AccountService {

    @Autowired private AccountRepository accountRepository;

    public Account save(Account account) {
        // 특정 계좌는 jsonConfig 설정을 추가하려고 함.

        if (account.getName().contains("ISA") || account.getName().contains("연금")) {
            if (account.getJsonConfig() == null) {
                account.setJsonConfig(new HashMap<>());
            }
            account.getJsonConfig().put("isTaxDeferred", true);
        }

        return accountRepository.save(account);
    }
}
