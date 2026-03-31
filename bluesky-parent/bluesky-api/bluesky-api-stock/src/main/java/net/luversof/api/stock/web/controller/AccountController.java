package net.luversof.api.stock.web.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.service.AccountService;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired private AccountService accountService;

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @GetMapping("/{id}")
    public Optional<Account> getAccountById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @GetMapping("/search/findByUserId/{userId}")
    public List<Account> getAccountsByUserId(@PathVariable UUID userId) {
        return accountService.findByUserId(userId);
    }
}
